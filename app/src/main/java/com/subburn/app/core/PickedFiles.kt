package com.subburn.app.core

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.antonkarpenko.ffmpegkit.FFmpegKitConfig
import java.io.File

data class PickedMedia(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long
)

data class PickedSubtitle(
    val uri: Uri,
    val displayName: String,
    /** Cached copy on internal storage — libass needs a real file path. */
    val cachedFile: File,
    val cueCount: Int
)

object PickedFiles {

    fun readMedia(context: Context, uri: Uri): PickedMedia {
        val (name, size) = queryNameAndSize(context, uri, fallback = "video.mkv")
        // MediaMetadataRetriever only became AutoCloseable on API 29, so release by hand.
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
        return PickedMedia(uri, name, size, duration)
    }

    fun readSubtitle(context: Context, uri: Uri): PickedSubtitle {
        val (name, _) = queryNameAndSize(context, uri, fallback = "subtitles.srt")
        val target = File(context.cacheDir, "subs/${sanitize(name)}").apply {
            parentFile?.mkdirs()
        }
        context.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        val cues = target.useLines { lines -> lines.count { it.contains(" --> ") } }
        return PickedSubtitle(uri, name, target, cues)
    }

    /** ffmpeg cannot open a content:// URI, but FFmpegKit maps it to `saf:`. */
    fun ffmpegInputPath(context: Context, uri: Uri): String =
        FFmpegKitConfig.getSafParameterForRead(context, uri)

    fun outputFile(context: Context, sourceName: String): File {
        val base = sourceName.substringBeforeLast('.', sourceName)
        val dir = File(context.getExternalFilesDir(null), "renders").apply { mkdirs() }
        return File(dir, "${sanitize(base)}_burned.mp4")
    }

    private fun sanitize(name: String) = name.replace(Regex("[^\\w.\\-]+"), "_")

    private fun queryNameAndSize(context: Context, uri: Uri, fallback: String): Pair<String, Long> {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: fallback else fallback
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                return name to size
            }
        }
        return fallback to 0L
    }
}

fun Long.asReadableSize(): String {
    if (this <= 0) return "—"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}

fun Long.asClock(): String {
    if (this <= 0) return "—"
    val totalSeconds = this / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
