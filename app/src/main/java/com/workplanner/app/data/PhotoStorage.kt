package com.workplanner.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Profile pictures are cropped to a circle by the UI and stored as small PNG
 * files in the app's private directory. Only the file path lives in the
 * repository document.
 */
object PhotoStorage {

    private const val DIRECTORY = "avatars"

    fun save(context: Context, personId: String, bitmap: Bitmap): String? {
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        // A fresh file name per update keeps the image cache in the UI honest.
        val file = File(directory, "$personId-${System.currentTimeMillis()}.png")
        return runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file.absolutePath
        }.getOrNull()
    }

    fun read(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        if (!File(path).isFile) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        // Avatars saved by older builds were masked with SRC_IN and came out as
        // a solid black circle. Show initials instead of that broken image.
        if (bitmap.isBlankBlackCircle()) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    private fun Bitmap.isBlankBlackCircle(): Boolean {
        val sample = Bitmap.createScaledBitmap(this, 16, 16, false)
        val pixels = IntArray(16 * 16)
        sample.getPixels(pixels, 0, 16, 0, 0, 16, 16)
        if (sample !== this) sample.recycle()

        pixels.forEach { pixel ->
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 200) return@forEach
            if ((pixel and 0x00FFFFFF) != 0) return false
        }
        return true
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
