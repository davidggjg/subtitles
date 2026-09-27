package com.subburn.app.core

/**
 * A fully resolved render request. Paths are whatever ffmpeg can open
 * directly: a `saf:` handle for the picked video, real files for the
 * subtitle and the output.
 */
data class BurnJob(
    val inputPath: String,
    val subtitlePath: String,
    val outputPath: String,
    val displayName: String,
    val fontsDir: String?,
    val crf: Int = 20,
    val preset: String = "medium",
    val durationMs: Long = 0L,
    val style: SubtitleStyle = SubtitleStyle()
)

object FfmpegCommand {

    /** libass parses `filename` itself, so `:` `\` and `'` must be escaped. */
    private fun escapeFilterPath(path: String): String =
        path.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'")

    /** מסנן ה־subtitles עם כל הסגנון — משותף לצריבה ולתצוגה המקדימה. */
    fun subtitlesFilter(job: BurnJob): String {
        return buildString {
            append("subtitles=")
            append(escapeFilterPath(job.subtitlePath))
            job.fontsDir?.takeIf { it.isNotBlank() }?.let {
                append(":fontsdir=").append(escapeFilterPath(it))
            }
            append(":force_style='").append(job.style.toForceStyle()).append("'")
        }
    }

    fun build(job: BurnJob): List<String> {
        val filter = subtitlesFilter(job)
        return listOf(
            "-hide_banner", "-y",
            "-i", job.inputPath,
            "-vf", filter,
            "-c:v", "libx265",
            "-preset", job.preset,
            "-crf", job.crf.toString(),
            "-tag:v", "hvc1",
            "-c:a", "copy",
            "-movflags", "+faststart",
            job.outputPath
        )
    }

    /** The same command as a copy-pasteable shell line, for the UI preview. */
    fun preview(job: BurnJob): String =
        "ffmpeg " + build(job).joinToString(" ") { arg ->
            if (arg.any { it == ' ' || it == '\'' }) "\"${arg.replace("\"", "\\\"")}\"" else arg
        }
}
