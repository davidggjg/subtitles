package com.subburn.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.subburn.app.core.BurnJob
import com.subburn.app.core.BurnState
import com.subburn.app.core.FfmpegCommand
import com.subburn.app.core.PickedFiles
import com.subburn.app.core.PickedMedia
import com.subburn.app.core.PickedSubtitle
import com.subburn.app.core.PreviewRenderer
import com.subburn.app.core.RenderState
import com.subburn.app.service.BurnService
import com.subburn.app.ui.BurnScreen
import com.subburn.app.ui.BurnSettings
import com.subburn.app.ui.SubBurnTheme
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestNotificationPermission()
        PreviewRenderer.clearCache(this)

        setContent {
            SubBurnTheme {
                // האפליקציה כולה בעברית, ולכן הפריסה תמיד מימין לשמאל.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        var video by remember { mutableStateOf<PickedMedia?>(null) }
        var subtitle by remember { mutableStateOf<PickedSubtitle?>(null) }
        var settings by remember { mutableStateOf(BurnSettings()) }

        var previewPositionMs by remember { mutableStateOf(0L) }
        var previewFrame by remember { mutableStateOf<File?>(null) }
        var previewLoading by remember { mutableStateOf(false) }
        var previewError by remember { mutableStateOf<String?>(null) }
        // עולה בכל בקשת רענון ידנית, כדי לאלץ חישוב מחדש של הפריים.
        var previewNonce by remember { mutableStateOf(0) }

        val renderState by BurnState.state.collectAsState()
        val logLines by BurnState.log.collectAsState()
        val running = renderState is RenderState.Running

        val videoPicker = rememberOpenDocument { uri ->
            persist(uri)
            val picked = runCatching { PickedFiles.readMedia(this, uri) }
                .onFailure { toast("לא הצלחתי לקרוא את קובץ הווידאו") }
                .getOrNull()
            video = picked
            // פתיחה בעשירית הראשונה של הסרט — שם כבר יש תמונה ולרוב גם דיאלוג.
            previewPositionMs = ((picked?.durationMs ?: 0L) / 10).coerceAtLeast(0L)
        }
        val subtitlePicker = rememberOpenDocument { uri ->
            subtitle = runCatching { PickedFiles.readSubtitle(this, uri) }
                .onFailure { toast("לא הצלחתי לקרוא את קובץ הכתוביות") }
                .getOrNull()
        }

        // תצוגה מקדימה אמיתית: מחכה שהמשתמשת תפסיק להזיז את הסליידר, ואז
        // מוציאה פריים אחד עם הכתוביות צרובות עליו.
        val currentVideo = video
        val currentSubtitle = subtitle
        LaunchedEffect(
            currentVideo?.uri,
            currentSubtitle?.cachedFile?.absolutePath,
            settings,
            previewPositionMs,
            previewNonce,
            running
        ) {
            if (currentVideo == null || currentSubtitle == null || running) return@LaunchedEffect
            delay(450)
            previewLoading = true
            previewError = null
            val job = buildJob(currentVideo, currentSubtitle, settings)
            val frame = runCatching {
                PreviewRenderer.renderFrame(this@MainActivity, job, previewPositionMs)
            }.getOrNull()
            previewLoading = false
            if (frame == null) {
                previewError = "לא הצלחתי להכין תצוגה מקדימה לנקודה הזאת"
            } else {
                previewFrame?.delete()
                previewFrame = frame
            }
        }

        BurnScreen(
            video = video,
            subtitle = subtitle,
            settings = settings,
            renderState = renderState,
            logLines = logLines,
            commandPreview = FfmpegCommand.preview(previewJob(video, subtitle, settings)),
            previewFrame = previewFrame,
            previewLoading = previewLoading,
            previewError = previewError,
            previewPositionMs = previewPositionMs,
            onPreviewPositionChange = { previewPositionMs = it },
            onPreviewRefresh = { previewNonce++ },
            onPickVideo = { videoPicker(arrayOf("video/*", "application/octet-stream")) },
            onPickSubtitle = { subtitlePicker(arrayOf("application/x-subrip", "text/*", "*/*")) },
            onSettingsChange = { settings = it },
            onReset = {
                video = null
                subtitle = null
                settings = BurnSettings()
                previewFrame = null
                previewPositionMs = 0L
                previewError = null
                PreviewRenderer.clearCache(this)
                BurnState.update(RenderState.Idle)
                BurnState.resetLog()
            },
            onStart = {
                val v = video ?: return@BurnScreen
                val s = subtitle ?: return@BurnScreen
                BurnService.start(this, buildJob(v, s, settings))
            },
            onCancel = { BurnService.cancel(this) }
        )
    }

    private fun buildJob(
        video: PickedMedia,
        subtitle: PickedSubtitle,
        settings: BurnSettings
    ): BurnJob {
        val output = PickedFiles.outputFile(this, video.displayName)
        return BurnJob(
            inputPath = PickedFiles.ffmpegInputPath(this, video.uri),
            subtitlePath = subtitle.cachedFile.absolutePath,
            outputPath = output.absolutePath,
            displayName = output.name,
            fontsDir = settings.fontsDir.ifBlank { null },
            crf = settings.crf,
            preset = settings.preset,
            durationMs = video.durationMs,
            style = settings.style
        )
    }

    /** מילוי מקומות לתצוגת הפקודה לפני שנבחרו קבצים. */
    private fun previewJob(
        video: PickedMedia?,
        subtitle: PickedSubtitle?,
        settings: BurnSettings
    ): BurnJob = BurnJob(
        inputPath = video?.displayName ?: "input_video.mkv",
        subtitlePath = subtitle?.displayName ?: "subtitles.srt",
        outputPath = video?.let { PickedFiles.outputFile(this, it.displayName).name }
            ?: "output_burned.mp4",
        displayName = "preview",
        fontsDir = settings.fontsDir.ifBlank { null },
        crf = settings.crf,
        preset = settings.preset,
        style = settings.style
    )

    private fun persist(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun rememberOpenDocument(onPicked: (Uri) -> Unit): (Array<String>) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPicked)
    }
    return { mimeTypes -> launcher.launch(mimeTypes) }
}
