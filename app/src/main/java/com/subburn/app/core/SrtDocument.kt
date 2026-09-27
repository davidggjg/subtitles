package com.subburn.app.core

import java.io.File

/** שורת כתובית אחת: מתי מופיעה, מתי נעלמת, ומה כתוב בה. */
data class Cue(
    val startMs: Long,
    val endMs: Long,
    val text: String
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * קריאה וכתיבה של קובצי SRT. הכתיבה תמיד מסודרת לפי זמן ומְמֻסְפרת מחדש,
 * כך שהקובץ שנצרב תקין גם אחרי עריכות והוספות.
 */
object SrtDocument {

    /** סימן ה־BOM שעורכי כתוביות בווינדוס נוהגים לשתול בתחילת הקובץ. */
    private const val BOM = "\uFEFF"

    private val timeLine = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    fun parse(file: File): List<Cue> {
        if (!file.exists()) return emptyList()
        val cues = mutableListOf<Cue>()
        var start = -1L
        var end = -1L
        val text = StringBuilder()

        fun flush() {
            if (start >= 0) {
                val body = text.toString().trim()
                if (body.isNotEmpty()) cues += Cue(start, end, body)
            }
            start = -1L
            end = -1L
            text.setLength(0)
        }

        // BOM ושורות מספור מסולקות; כל בלוק מסתיים בשורה ריקה.
        file.readLines(Charsets.UTF_8).forEach { raw ->
            val line = raw.removePrefix(BOM).trimEnd()
            val match = timeLine.find(line)
            when {
                match != null -> {
                    flush()
                    val g = match.groupValues
                    start = toMs(g[1], g[2], g[3], g[4])
                    end = toMs(g[5], g[6], g[7], g[8])
                }
                line.isBlank() -> flush()
                start >= 0 -> {
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(line)
                }
                // שורת מספור לפני חותמת הזמן — מדלגים עליה.
                else -> Unit
            }
        }
        flush()
        return cues.sortedBy { it.startMs }
    }

    fun write(cues: List<Cue>, target: File) {
        target.parentFile?.mkdirs()
        val body = buildString {
            cues.sortedBy { it.startMs }.forEachIndexed { index, cue ->
                append(index + 1).append('\n')
                append(format(cue.startMs)).append(" --> ").append(format(cue.endMs)).append('\n')
                append(cue.text.trim()).append("\n\n")
            }
        }
        target.writeText(body, Charsets.UTF_8)
    }

    /** הכתובית שאמורה להופיע ברגע נתון, אם יש כזו. */
    fun cueAt(cues: List<Cue>, positionMs: Long): Cue? =
        cues.lastOrNull { positionMs >= it.startMs && positionMs <= it.endMs }

    private fun toMs(h: String, m: String, s: String, millis: String): Long =
        h.toLong() * 3_600_000 + m.toLong() * 60_000 + s.toLong() * 1_000 +
            millis.padEnd(3, '0').take(3).toLong()

    private fun format(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val h = safe / 3_600_000
        val m = (safe % 3_600_000) / 60_000
        val s = (safe % 60_000) / 1_000
        val millis = safe % 1_000
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }
}
