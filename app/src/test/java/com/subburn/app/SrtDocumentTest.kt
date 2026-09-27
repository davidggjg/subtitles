package com.subburn.app

import com.subburn.app.core.BackgroundMode
import com.subburn.app.core.Cue
import com.subburn.app.core.SrtDocument
import com.subburn.app.core.SubtitleStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SrtDocumentTest {

    @get:Rule
    val folder = TemporaryFolder()

    // רצף בריחה ולא התו עצמו, כדי לא לשתול BOM בתוך קובץ המקור.
    private val bom = "\uFEFF"

    private val sample = """
        ${bom}1
        00:00:01,000 --> 00:00:03,500
        שלום עולם

        2
        00:01:02,250 --> 00:01:04,000
        שורה ראשונה
        שורה שנייה

    """.trimIndent()

    @Test
    fun `parses numbering, bom and multi-line cues`() {
        val file = folder.newFile("in.srt").apply { writeText(sample) }
        val cues = SrtDocument.parse(file)

        assertEquals(2, cues.size)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals(3_500L, cues[0].endMs)
        assertEquals("שלום עולם", cues[0].text)
        assertEquals(62_250L, cues[1].startMs)
        assertEquals("שורה ראשונה\nשורה שנייה", cues[1].text)
    }

    @Test
    fun `write then parse returns the same cues, renumbered and sorted`() {
        val target = folder.newFile("out.srt")
        val cues = listOf(
            Cue(5_000, 6_000, "שנייה בזמן"),
            Cue(1_500, 2_000, "ראשונה בזמן")
        )

        SrtDocument.write(cues, target)
        val text = target.readText()
        assertTrue(text.startsWith("1\n00:00:01,500 --> 00:00:02,000"))
        assertEquals(cues.sortedBy { it.startMs }, SrtDocument.parse(target))
    }

    @Test
    fun `cueAt finds the cue covering a position and nothing in the gaps`() {
        val cues = listOf(Cue(1_000, 2_000, "א"), Cue(3_000, 4_000, "ב"))

        assertEquals("א", SrtDocument.cueAt(cues, 1_500)?.text)
        assertEquals("ב", SrtDocument.cueAt(cues, 4_000)?.text)
        assertNull(SrtDocument.cueAt(cues, 2_500))
        assertNull(SrtDocument.cueAt(cues, 0))
    }
}

class SubtitleStyleTest {

    @Test
    fun `force style uses ass bgr colours and an inverted alpha byte`() {
        val style = SubtitleStyle(backgroundOpacity = 100)

        val forced = style.toForceStyle()

        assertTrue(forced.contains("PrimaryColour=&H00FFFFFF"))
        assertTrue(forced.contains("OutlineColour=&H00000000"))
        // 100% אטימות => בייט אלפא 0 בתחביר של ASS.
        assertTrue(forced.contains("BackColour=&H00000000"))
    }

    @Test
    fun `half transparent box keeps a mid alpha byte`() {
        val forced = SubtitleStyle(backgroundOpacity = 50).toForceStyle()

        assertTrue(forced.contains("BackColour=&H80000000"))
    }

    @Test
    fun `box mode maps to border style three and blur is omitted when off`() {
        val boxed = SubtitleStyle(background = BackgroundMode.BOX, blur = 0f).toForceStyle()
        val blurred = SubtitleStyle(blur = 1.5f).toForceStyle()

        assertTrue(boxed.contains("BorderStyle=3"))
        assertTrue(!boxed.contains("Blur="))
        assertTrue(blurred.contains("Blur=1.5"))
    }

    @Test
    fun `italic and bold are emitted as ass flags`() {
        val forced = SubtitleStyle(italic = true, bold = true).toForceStyle()

        assertTrue(forced.contains("Italic=1"))
        assertTrue(forced.contains("Bold=1"))
    }
}
