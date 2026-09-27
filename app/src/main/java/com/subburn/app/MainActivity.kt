package com.subburn.app

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.subburn.app.core.BurnJob
import com.subburn.app.core.BurnState
import com.subburn.app.core.FfmpegCommand
import com.subburn.app.core.PickedFiles
import com.subburn.app.core.PickedMedia
import com.subburn.app.core.PickedSubtitle
import com.subburn.app.core.RenderState
import com.subburn.app.service.BurnService
import com.subburn.app.ui.BurnScreen
import com.subburn.app.ui.BurnSettings
import com.subburn.app.ui.SubBurnTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestNotificationPermission()

        setContent {
            SubBurnTheme {
                var video by remember { mutableStateOf<PickedMedia?>(null) }
                var subtitle by remember { mutableStateOf<PickedSubtitle?>(null) }
                var settings by remember { mutableStateOf(BurnSettings()) }

                val renderState by BurnState.state.collectAsState()
                val logLines by BurnState.log.collectAsState()

                val videoPicker = rememberOpenDocument { uri ->
                    persist(uri)
                    video = runCatching { PickedFiles.readMedia(this, uri) }
                        .onFailure { toast("Could not read that video") }
                        .getOrNull()
                }
                val subtitlePicker = rememberOpenDocument { uri ->
                    subtitle = runCatching { PickedFiles.readSubtitle(this, uri) }
                        .onFailure { toast("Could not read that subtitle file") }
                        .getOrNull()
                }

                BurnScreen(
                    video = video,
                    subtitle = subtitle,
                    settings = settings,
                    renderState = renderState,
                    logLines = logLines,
                    commandPreview = FfmpegCommand.preview(
                        previewJob(video, subtitle, settings)
                    ),
                    onPickVideo = { videoPicker(arrayOf("video/*", "application/octet-stream")) },
                    onPickSubtitle = {
                        subtitlePicker(arrayOf("application/x-subrip", "text/*", "*/*"))
                    },
                    onSettingsChange = { settings = it },
                    onStart = {
                        val currentVideo = video ?: return@BurnScreen
                        val currentSubtitle = subtitle ?: return@BurnScreen
                        BurnService.start(this, buildJob(currentVideo, currentSubtitle, settings))
                    },
                    onCancel = { BurnService.cancel(this) }
                )
            }
        }
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

    /** A stand-in job so the command preview renders before both files are picked. */
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
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberOpenDocument(onPicked: (Uri) -> Unit): (Array<String>) -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPicked) }
    return { mimeTypes -> launcher.launch(mimeTypes) }
}
