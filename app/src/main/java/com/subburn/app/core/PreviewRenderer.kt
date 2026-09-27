package com.subburn.app.core

import android.content.Context
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * מוציא פריים בודד מתוך הווידאו עם הכתוביות צרובות עליו, באותו מסנן שישמש
 * לצריבה האמיתית — כך שמה שרואים בתצוגה המקדימה הוא מה שיתקבל בפועל.
 */
object PreviewRenderer {

    suspend fun renderFrame(context: Context, job: BurnJob, positionMs: Long): File? {
        val target = File(context.cacheDir, "preview/frame_${System.currentTimeMillis()}.jpg")
            .apply { parentFile?.mkdirs() }
        val seconds = "%.3f".format(positionMs / 1000.0)
        val arguments = arrayOf(
            "-hide_banner", "-y",
            // חיפוש לפני הקלט הוא המהיר; copyts שומר על חותמות הזמן המקוריות
            // כדי שהכתובית הנכונה תופיע על הפריים.
            "-ss", seconds,
            "-copyts",
            "-i", job.inputPath,
            "-vf", "${FfmpegCommand.subtitlesFilter(job)},scale=720:-2",
            "-frames:v", "1",
            "-q:v", "3",
            target.absolutePath
        )
        val ok = suspendCancellableCoroutine { continuation ->
            val session = FFmpegKit.executeWithArgumentsAsync(arguments) { completed ->
                if (continuation.isActive) {
                    continuation.resume(ReturnCode.isSuccess(completed.returnCode))
                }
            }
            continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
        }
        return target.takeIf { ok && it.length() > 0 } ?: run { target.delete(); null }
    }

    /** התצוגה המקדימה כותבת פריימים ל־cache; מנקה כדי שלא יצטברו. */
    fun clearCache(context: Context) {
        File(context.cacheDir, "preview").listFiles()?.forEach { it.delete() }
    }
}
