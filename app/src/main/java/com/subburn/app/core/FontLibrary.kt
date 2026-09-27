package com.subburn.app.core

import android.content.Context
import android.util.Log
import com.antonkarpenko.ffmpegkit.FFmpegKitConfig
import java.io.File

/**
 * הפונט העברי נארז בתוך האפליקציה.
 *
 * libass לא מחפש פונטים בעצמו במכשיר: בלי תיקיית פונטים שמכילה אות עברית
 * הכתוביות יוצאות ריקות או ריבועים. לכן הפונט מועתק בהפעלה הראשונה לאחסון
 * הפרטי של האפליקציה, והתיקייה הזו נמסרת גם ל־fontconfig וגם לאפשרות
 * fontsdir של המסנן.
 */
object FontLibrary {

    /** שם המשפחה כפי ש־libass יחפש אותו ב־FontName. */
    const val FAMILY = "Noto Sans Hebrew"

    private const val ASSET_DIR = "fonts"
    private val FONT_FILES = listOf(
        "NotoSansHebrew-Regular.ttf",
        "NotoSansHebrew-Bold.ttf"
    )

    /** מעתיק את הפונטים המצורפים (פעם אחת) ומחזיר את התיקייה שלהם. */
    fun ensure(context: Context): File {
        val dir = File(context.filesDir, ASSET_DIR).apply { mkdirs() }
        FONT_FILES.forEach { name ->
            val target = File(dir, name)
            val expected = runCatching {
                context.assets.openFd("$ASSET_DIR/$name").use { it.length }
            }.getOrDefault(-1L)
            if (target.exists() && (expected < 0 || target.length() == expected)) return@forEach
            runCatching {
                context.assets.open("$ASSET_DIR/$name").use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }.onFailure { Log.w("FontLibrary", "לא הצלחתי להעתיק את $name", it) }
        }
        return dir
    }

    /**
     * רושם את תיקיות הפונטים ל־fontconfig. גם פונטי המערכת נכללים, כדי
     * שפונט שהמשתמשת תבקש בשם ימצא אם הוא מותקן במכשיר.
     */
    fun configure(context: Context, extraDirectory: String? = null) {
        val directories = buildList {
            add(ensure(context).absolutePath)
            add("/system/fonts")
            extraDirectory?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        runCatching { FFmpegKitConfig.setFontDirectoryList(context, directories, emptyMap()) }
            .onFailure { Log.w("FontLibrary", "רישום תיקיות הפונטים נכשל", it) }
    }

    /** התיקייה שתימסר ל־fontsdir כשהמשתמשת לא בחרה תיקייה משלה. */
    fun defaultFontsDir(context: Context): String = ensure(context).absolutePath
}
