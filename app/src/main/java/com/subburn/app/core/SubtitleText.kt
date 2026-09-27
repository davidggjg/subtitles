package com.subburn.app.core

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * קריאת קובץ כתוביות בלי להניח קידוד.
 *
 * הרבה קובצי SRT בעברית נשמרו ב־windows-1255 ולא ב־UTF-8. אם קוראים אותם
 * כ־UTF-8 מתקבל ג'יבריש, והוא נצרב ככה לתוך הווידאו. לכן מנסים קודם UTF-8
 * בקפדנות, ורק אם הבתים לא חוקיים עוברים לקידוד העברי הישן.
 */
object SubtitleText {

    private const val BOM = "\uFEFF"

    /** שמות אפשריים לקידוד העברי הישן; לא כל מכשיר מכיר את שניהם. */
    private val HEBREW_FALLBACKS = listOf("windows-1255", "ISO-8859-8")

    fun read(file: File): String = decode(file.readBytes())

    fun decode(bytes: ByteArray): String {
        strictUtf8(bytes)?.let { return it.removePrefix(BOM) }
        HEBREW_FALLBACKS.forEach { name ->
            val charset = runCatching { Charset.forName(name) }.getOrNull() ?: return@forEach
            return String(bytes, charset).removePrefix(BOM)
        }
        return String(bytes, Charsets.UTF_8).removePrefix(BOM)
    }

    /** מחזיר null כשהבתים אינם UTF-8 תקין, במקום להחליף תווים בסימני שאלה. */
    private fun strictUtf8(bytes: ByteArray): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()
}
