package com.subburn.app

import com.subburn.app.core.TimelineMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineMathTest {

    /** 46dp לשנייה, במסך בצפיפות גבוהה (xxhdpi). */
    private val secondWidthPx = 46f * 3f

    @Test
    fun `a chunk stays far below the layout width ceiling on a dense screen`() {
        assertTrue(TimelineMath.chunkWidthPx(secondWidthPx) < TimelineMath.MAX_LAYOUT_PX)
    }

    @Test
    fun `a feature-length film is split into chunks instead of one long strip`() {
        // הסרט שהפיל את הגרסה הקודמת: שעה וחצי ועוד.
        val durationMs = 98L * 60 * 1000
        val chunks = TimelineMath.chunkCount(durationMs)

        assertEquals(588, chunks)
        // הרצועה הרציפה שהחליפו המקטעים הייתה חורגת מהתקרה.
        assertTrue(durationMs / 1000f * secondWidthPx > TimelineMath.MAX_LAYOUT_PX)
    }

    @Test
    fun `an empty or unknown duration still yields one chunk`() {
        assertEquals(1, TimelineMath.chunkCount(0))
        assertEquals(1, TimelineMath.chunkCount(-5))
    }

    @Test
    fun `scroll position and playback position are inverses of each other`() {
        val pxPerMs = secondWidthPx / 1000f
        val chunkPx = TimelineMath.chunkWidthPx(secondWidthPx)
        val positionMs = 754_300L

        val scrolled = TimelineMath.scrollPx(positionMs, pxPerMs)
        val index = (scrolled / chunkPx).toInt()
        val offset = (scrolled % chunkPx).toInt()

        assertEquals(
            positionMs.toFloat(),
            TimelineMath.positionMs(index, offset, chunkPx, pxPerMs).toFloat(),
            10f
        )
    }
}
