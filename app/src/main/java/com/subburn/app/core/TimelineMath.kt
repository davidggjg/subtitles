package com.subburn.app.core

import kotlin.math.ceil

/**
 * החישובים של פס הזמן, בנפרד מהתצוגה כדי שיהיה אפשר לבדוק אותם.
 *
 * הפס מחולק למקטעים באורך קבוע. זה לא קישוט: לפריסה של Compose יש תקרת
 * רוחב של 262,143 פיקסלים, ורצועה אחת רציפה של סרט ארוך חורגת ממנה ומפילה
 * את האפליקציה. מקטע בגודל קבוע לא תלוי באורך הסרט.
 */
object TimelineMath {

    const val CHUNK_SECONDS = 10
    const val CHUNK_MS = CHUNK_SECONDS * 1000L

    /** התקרה שממנה Compose זורק חריגה בזמן מדידה. */
    const val MAX_LAYOUT_PX = 262_143

    fun chunkCount(durationMs: Long): Int =
        ceil(durationMs.coerceAtLeast(0L).toDouble() / CHUNK_MS).toInt().coerceAtLeast(1)

    /** מיקום הגלילה (בפיקסלים) שמתאים לרגע נתון בסרט. */
    fun scrollPx(positionMs: Long, pxPerMs: Float): Float =
        positionMs.coerceAtLeast(0L) * pxPerMs

    /** הרגע בסרט שמתאים למקטע ולהיסט גלילה בתוכו. */
    fun positionMs(chunkIndex: Int, offsetPx: Int, chunkPx: Float, pxPerMs: Float): Long {
        if (pxPerMs <= 0f) return 0L
        return ((chunkIndex * chunkPx + offsetPx) / pxPerMs).toLong()
    }

    /** רוחב מקטע אחד בפיקסלים — חייב להישאר מתחת לתקרה בכל צפיפות מסך. */
    fun chunkWidthPx(secondWidthPx: Float): Float = secondWidthPx * CHUNK_SECONDS
}
