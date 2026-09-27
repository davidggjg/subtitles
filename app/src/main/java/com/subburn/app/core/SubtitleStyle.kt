package com.subburn.app.core

/** טיפול הרקע מאחורי הכתובית. */
enum class BackgroundMode(val label: String, val hint: String, val borderStyle: Int) {
    /** BorderStyle=1 — אותיות עם קו מסגרת וצל, בלי מלבן. */
    OUTLINE("מסגרת בלבד", "האותיות עטופות בשחור, הרקע נשאר שקוף", 1),

    /** BorderStyle=3 — מלבן אטום/שקוף למחצה מאחורי כל שורה. */
    BOX("רקע מלא", "מלבן שחור מאחורי הטקסט — קריא גם על רקע בהיר", 3)
}

/**
 * כל מה שנכנס ל־force_style של libass. כל שדה מתורגם אחד־לאחד לשדה סגנון
 * ASS, כך שהמחרוזת שנוצרת כאן היא בדיוק מה שffmpeg מקבל.
 */
data class SubtitleStyle(
    val fontName: String = "Noto Sans Hebrew",
    val fontSize: Int = 20,
    val background: BackgroundMode = BackgroundMode.OUTLINE,
    val outline: Float = 1.5f,
    val shadow: Float = 0.5f,
    /** 0..100 — כמה אטום המלבן או הצל שמאחורי הטקסט. */
    val backgroundOpacity: Int = 70,
    val bold: Boolean = false,
    /** הטיה באלכסון — האותיות נשענות קדימה והשחור סביבן נראה מקצועי יותר. */
    val italic: Boolean = false,
    /** ריכוך הקצוות של המסגרת והצל (libass Blur), 0 = קצה חד. */
    val blur: Float = 0f,
    val marginV: Int = 24
) {
    /** &HAABBGGRR — צבעי ASS הם BGR עם בייט אלפא הפוך. */
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
        add("Italic=${if (italic) 1 else 0}")
        if (blur > 0f) add("Blur=${trim(blur)}")
        add("MarginV=$marginV")
    }.joinToString(",")

    private fun trim(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)
}
