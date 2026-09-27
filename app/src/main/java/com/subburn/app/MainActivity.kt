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
import com.subburn.app.core.Cue
import com.subburn.app.core.FfmpegCommand
import com.subburn.app.core.PickedFiles
import com.subburn.app.core.PickedMedia
import com.subburn.app.core.RenderState
import com.subburn.app.core.SrtDocument
import com.subburn.app.service.BurnService
import com.subburn.app.ui.BurnScreen
import com.subburn.app.ui.BurnSettings
import com.subburn.app.ui.SubBurnTheme
import com.subburn.app.ui.rememberPlaybackState
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestNotificationPermission()

        setContent {
            SubBurnTheme {
                // הממשק כולו בעברית, ולכן הפריסה תמיד מימין לשמאל.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        var video by remember { mutableStateOf<PickedMedia?>(null) }
        var subtitleName by remember { mutableStateOf<String?>(null) }
        var cues by remember { mutableStateOf<List<Cue>>(emptyList()) }
        var settings by remember { mutableStateOf(BurnSettings()) }

        val playback = rememberPlaybackState()
        val renderState by BurnState.state.collectAsState()
        val logLines by BurnState.log.collectAsState()

        val videoPicker = rememberOpenDocument { uri ->
            persist(uri)
            val picked = runCatching { PickedFiles.readMedia(this, uri) }
                .onFailure { toast("לא הצלחתי לקרוא את קובץ הווידאו") }
                .getOrNull()
            video = picked
            playback.durationMs = picked?.durationMs ?: 0L
            playback.positionMs = 0L
        }
        val subtitlePicker = rememberOpenDocument { uri ->
            val picked = runCatching { PickedFiles.readSubtitle(this, uri) }
                .onFailure { toast("לא הצלחתי לקרוא את קובץ הכתוביות") }
                .getOrNull()
            if (picked != null) {
                subtitleName = picked.displayName
                cues = SrtDocument.parse(picked.cachedFile)
                if (cues.isEmpty()) toast("לא נמצאו כתוביות בקובץ הזה")
            }
        }

        BurnScreen(
            video = video,
            subtitleName = subtitleName,
            cues = cues,
            settings = settings,
            playback = playback,
            renderState = renderState,
            logLines = logLines,
            commandPreview = FfmpegCommand.preview(previewJob(video, subtitleName, settings)),
            onPickVideo = { videoPicker(arrayOf("video/*", "application/octet-stream")) },
            onPickSubtitle = { subtitlePicker(arrayOf("application/x-subrip", "text/*", "*/*")) },
            onSettingsChange = { settings = it },
            onCueSave = { index, cue ->
                cues = if (index == null) {
                    (cues + cue).sortedBy { it.startMs }
                } else {
                    cues.toMutableList().apply { this[index] = cue }.sortedBy { it.startMs }
                }
                if (subtitleName == null) subtitleName = "כתוביות שלי"
            },
            onCueDelete = { index ->
                cues = cues.toMutableList().apply { removeAt(index) }
            },
            onReset = {
                playback.pause()
                video = null
                subtitleName = null
                cues = emptyList()
                settings = BurnSettings()
                BurnState.update(RenderState.Idle)
                BurnState.resetLog()
            },
            onStart = {
                val currentVideo = video ?: return@BurnScreen
                if (cues.isEmpty()) return@BurnScreen
                // הכתוביות נכתבות מחדש מהעריכות שבמסך — זה הקובץ שנצרב.
                val srt = File(cacheDir, "subs/burn.srt")
                runCatching { SrtDocument.write(cues, srt) }
                    .onFailure { toast("לא הצלחתי לשמור את הכתוביות לצריבה") }
                    .onSuccess { BurnService.start(this, buildJob(currentVideo, srt, settings)) }
            },
            onCancel = { BurnService.cancel(this) }
        )
    }

    private fun buildJob(video: PickedMedia, srt: File, settings: BurnSettings): BurnJob {
        val output = PickedFiles.outputFile(this, video.displayName)
        return BurnJob(
            inputPath = PickedFiles.ffmpegInputPath(this, video.uri),
            subtitlePath = srt.absolutePath,
            outputPath = output.absolutePath,
            displayName = output.name,
            fontsDir = settings.fontsDir.ifBlank { null },
            crf = settings.crf,
            preset = settings.preset,
            durationMs = video.durationMs,
            style = settings.style
        )
    }

    /** גרסה להצגה בלבד של הפקודה, לפני שנבחרו קבצים. */
    private fun previewJob(
        video: PickedMedia?,
        subtitleName: String?,
        settings: BurnSettings
    ): BurnJob = BurnJob(
        inputPath = video?.displayName ?: "input_video.mkv",
        subtitlePath = subtitleName ?: "subtitles.srt",
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
