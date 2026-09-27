package com.subburn.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.FFmpegSession
import com.antonkarpenko.ffmpegkit.Level
import com.antonkarpenko.ffmpegkit.ReturnCode
import com.antonkarpenko.ffmpegkit.Statistics
import com.subburn.app.MainActivity
import com.subburn.app.R
import com.subburn.app.SubBurnApp
import com.subburn.app.core.BurnJob
import com.subburn.app.core.BurnState
import com.subburn.app.core.FfmpegCommand
import com.subburn.app.core.MediaExporter
import com.subburn.app.core.RenderState
import com.subburn.app.core.SubtitleStyle
import java.io.File
import kotlin.math.roundToLong

/**
 * Runs the burn-in as a foreground service holding a partial WakeLock, so the
 * encode keeps going at full speed with the screen off or the app swiped away.
 */
class BurnService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var session: FFmpegSession? = null
    private var job: BurnJob? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelRender()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val parsed = intent.toBurnJob()
                if (parsed == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRender(parsed)
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRender(request: BurnJob) {
        if (session != null) return
        job = request
        BurnState.resetLog()
        BurnState.update(
            RenderState.Running(0f, 0.0, 0.0, 0L, -1L, request.displayName)
        )
        startForeground(NOTIFICATION_ID, buildNotification(0f, "Starting encode…"))
        acquireWakeLock()

        session = FFmpegKit.executeWithArgumentsAsync(
            FfmpegCommand.build(request).toTypedArray(),
            { completed -> onFinished(request, completed.returnCode, File(request.outputPath)) },
            { log ->
                if (log.level != Level.AV_LOG_STDERR) BurnState.appendLog(log.message.trimEnd())
            },
            { stats -> onStatistics(request, stats) }
        )
    }

    private fun onStatistics(request: BurnJob, stats: Statistics) {
        val current = BurnState.state.value as? RenderState.Running ?: return
        val timeMs = stats.time.toDouble()
        val progress = if (request.durationMs > 0) {
            (timeMs / request.durationMs).toFloat().coerceIn(0f, 1f)
        } else {
            -1f
        }
        val speed = stats.speed
        val remainingSeconds = ((request.durationMs - timeMs).coerceAtLeast(0.0)) / 1000.0
        val eta = if (speed > 0.01 && request.durationMs > 0) {
            (remainingSeconds / speed).roundToLong()
        } else {
            -1L
        }
        BurnState.update(
            current.copy(
                progress = progress,
                speed = speed,
                fps = stats.videoFps.toDouble(),
                outputSizeBytes = stats.size.toLong(),
                etaSeconds = eta
            )
        )
        updateNotification(progress, formatTicker(speed, eta))
    }

    private fun onFinished(request: BurnJob, code: ReturnCode?, output: File) {
        val state = when {
            ReturnCode.isSuccess(code) -> {
                val savedTo = MediaExporter.publishToMovies(this, output, request.displayName)
                RenderState.Done(output.absolutePath, savedTo, output.length())
            }
            ReturnCode.isCancel(code) -> {
                output.delete()
                RenderState.Cancelled
            }
            else -> {
                output.delete()
                val tail = BurnState.log.value.lastOrNull { it.isNotBlank() } ?: "unknown error"
                RenderState.Failed(tail)
            }
        }
        BurnState.update(state)
        session = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cancelRender() {
        session?.let { FFmpegKit.cancel(it.sessionId) }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SubBurn::render").also {
            it.setReferenceCounted(false)
            it.acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun formatTicker(speed: Double, etaSeconds: Long): String {
        val speedText = "%.2fx".format(speed)
        if (etaSeconds < 0) return "Encoding · $speedText"
        val minutes = etaSeconds / 60
        val seconds = etaSeconds % 60
        return "Encoding · $speedText · ${minutes}m ${seconds}s left"
    }

    private fun buildNotification(progress: Float, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this, 1, Intent(this, BurnService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val indeterminate = progress < 0f
        return NotificationCompat.Builder(this, SubBurnApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_burn)
            .setContentTitle(job?.displayName ?: "Burning subtitles")
            .setContentText(text)
            .setProgress(100, (progress.coerceAtLeast(0f) * 100).toInt(), indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Cancel", cancelIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(progress: Float, text: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationAt < 1000) return
        lastNotificationAt = now
        // On API 33+ the user can deny notifications; the encode still runs, it
        // just stops reporting progress in the shade.
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val manager = NotificationManagerCompat.from(this)
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(progress, text)) }
    }

    private var lastNotificationAt = 0L

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun Intent.toBurnJob(): BurnJob? {
        val input = getStringExtra(EXTRA_INPUT) ?: return null
        val srt = getStringExtra(EXTRA_SRT) ?: return null
        val output = getStringExtra(EXTRA_OUTPUT) ?: return null
        return BurnJob(
            inputPath = input,
            subtitlePath = srt,
            outputPath = output,
            displayName = getStringExtra(EXTRA_NAME) ?: File(output).name,
            fontsDir = getStringExtra(EXTRA_FONTS_DIR),
            crf = getIntExtra(EXTRA_CRF, 20),
            preset = getStringExtra(EXTRA_PRESET) ?: "medium",
            durationMs = getLongExtra(EXTRA_DURATION, 0L),
            style = SubtitleStyle(
                fontName = getStringExtra(EXTRA_FONT) ?: "Noto Sans Hebrew",
                fontSize = getIntExtra(EXTRA_FONT_SIZE, 20),
                background = com.subburn.app.core.BackgroundMode.valueOf(
                    getStringExtra(EXTRA_BACKGROUND) ?: com.subburn.app.core.BackgroundMode.OUTLINE.name
                ),
                outline = getFloatExtra(EXTRA_OUTLINE, 1.5f),
                shadow = getFloatExtra(EXTRA_SHADOW, 0.5f),
                backgroundOpacity = getIntExtra(EXTRA_OPACITY, 70),
                bold = getBooleanExtra(EXTRA_BOLD, false),
                marginV = getIntExtra(EXTRA_MARGIN, 24)
            )
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        private const val WAKELOCK_TIMEOUT_MS = 12L * 60 * 60 * 1000

        const val ACTION_START = "com.subburn.app.START"
        const val ACTION_CANCEL = "com.subburn.app.CANCEL"

        private const val EXTRA_INPUT = "input"
        private const val EXTRA_SRT = "srt"
        private const val EXTRA_OUTPUT = "output"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_FONTS_DIR = "fontsDir"
        private const val EXTRA_CRF = "crf"
        private const val EXTRA_PRESET = "preset"
        private const val EXTRA_DURATION = "duration"
        private const val EXTRA_FONT = "font"
        private const val EXTRA_FONT_SIZE = "fontSize"
        private const val EXTRA_BACKGROUND = "background"
        private const val EXTRA_OUTLINE = "outline"
        private const val EXTRA_SHADOW = "shadow"
        private const val EXTRA_OPACITY = "opacity"
        private const val EXTRA_BOLD = "bold"
        private const val EXTRA_MARGIN = "margin"

        fun start(context: Context, job: BurnJob) {
            val intent = Intent(context, BurnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INPUT, job.inputPath)
                putExtra(EXTRA_SRT, job.subtitlePath)
                putExtra(EXTRA_OUTPUT, job.outputPath)
                putExtra(EXTRA_NAME, job.displayName)
                putExtra(EXTRA_FONTS_DIR, job.fontsDir)
                putExtra(EXTRA_CRF, job.crf)
                putExtra(EXTRA_PRESET, job.preset)
                putExtra(EXTRA_DURATION, job.durationMs)
                putExtra(EXTRA_FONT, job.style.fontName)
                putExtra(EXTRA_FONT_SIZE, job.style.fontSize)
                putExtra(EXTRA_BACKGROUND, job.style.background.name)
                putExtra(EXTRA_OUTLINE, job.style.outline)
                putExtra(EXTRA_SHADOW, job.style.shadow)
                putExtra(EXTRA_OPACITY, job.style.backgroundOpacity)
                putExtra(EXTRA_BOLD, job.style.bold)
                putExtra(EXTRA_MARGIN, job.style.marginV)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, BurnService::class.java).setAction(ACTION_CANCEL)
            )
        }
    }
}
