package com.subburn.app.core

/** Background treatment applied behind the subtitle text. */
enum class BackgroundMode(val label: String, val borderStyle: Int) {
    /** BorderStyle=1 — outlined glyphs with a drop shadow, no box. */
    OUTLINE("Outline only", 1),

    /** BorderStyle=3 — solid/translucent box drawn behind every line. */
    BOX("Full box", 3)
}

/**
 * Everything the libass `force_style` option needs. Values map 1:1 onto ASS
 * style fields, so the string this produces is what ffmpeg understands.
 */
data class SubtitleStyle(
    val fontName: String = "Noto Sans Hebrew",
    val fontSize: Int = 20,
    val background: BackgroundMode = BackgroundMode.OUTLINE,
    val outline: Float = 1.5f,
    val shadow: Float = 0.5f,
    /** 0..100, how opaque the box or shadow backdrop is. */
    val backgroundOpacity: Int = 70,
    val bold: Boolean = false,
    val marginV: Int = 24
) {
    /** &HAABBGGRR — ASS colours are BGR with an inverted alpha byte. */
    private fun assColour(rgb: Int, opacityPercent: Int): String {
        val alpha = (255 - (opacityPercent.coerceIn(0, 100) * 255 / 100)) and 0xFF
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return "&H%02X%02X%02X%02X".format(alpha, b, g, r)
    }

    fun toForceStyle(): String = buildList {
        add("FontName=$fontName")
        add("FontSize=$fontSize")
        add("PrimaryColour=${assColour(0xFFFFFF, 100)}")
        add("OutlineColour=${assColour(0x000000, 100)}")
        add("BackColour=${assColour(0x000000, backgroundOpacity)}")
        add("BorderStyle=${background.borderStyle}")
        add("Outline=${trim(outline)}")
        add("Shadow=${trim(shadow)}")
        add("Bold=${if (bold) 1 else 0}")
        add("MarginV=$marginV")
    }.joinToString(",")

    private fun trim(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)
}
