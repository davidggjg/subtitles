package com.subburn.app.core

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaExporter {

    /**
     * Copies a finished render into the shared Movies/SubBurn folder so it shows
     * up in the gallery. Returns the public path, or null if publishing failed
     * (the file is still readable at its app-private location either way).
     */
    fun publishToMovies(context: Context, source: File, displayName: String): String? {
        if (!source.exists()) return null
        val relativeDir = "${Environment.DIRECTORY_MOVIES}/SubBurn"
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativeDir)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)!!.use { out -> source.inputStream().use { it.copyTo(out) } }
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                    null,
                    null
                )
                source.delete()
                "$relativeDir/$displayName"
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SubBurn")
                dir.mkdirs()
                val target = File(dir, displayName)
                source.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
                source.delete()
                target.absolutePath
            }
        }.getOrNull()
    }
}
