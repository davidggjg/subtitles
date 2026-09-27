package com.subburn.app

import com.subburn.app.core.SrtDocument
import com.subburn.app.core.SubtitleText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.Charset

class SubtitleTextTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val hebrew = "ברוך הבא למסיבה. מה שלומך?"

    @Test
    fun `utf-8 subtitles are read as they are`() {
        assertEquals(hebrew, SubtitleText.decode(hebrew.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `windows-1255 subtitles are recovered instead of turning into gibberish`() {
        val legacy = hebrew.toByteArray(Charset.forName("windows-1255"))

        assertEquals(hebrew, SubtitleText.decode(legacy))
    }

    @Test
    fun `a byte-order mark is not left at the start of the first cue`() {
        val withBom = ("\uFEFF" + hebrew).toByteArray(Charsets.UTF_8)

        assertEquals(hebrew, SubtitleText.decode(withBom))
    }

    @Test
    fun `a windows-1255 srt file parses into readable hebrew cues`() {
        val srt = "1\n00:00:01,000 --> 00:00:03,000\n$hebrew\n\n"
        val file = folder.newFile("legacy.srt")
        file.writeBytes(srt.toByteArray(Charset.forName("windows-1255")))

        val cues = SrtDocument.parse(file)

        assertEquals(1, cues.size)
        assertEquals(hebrew, cues[0].text)
    }
}
