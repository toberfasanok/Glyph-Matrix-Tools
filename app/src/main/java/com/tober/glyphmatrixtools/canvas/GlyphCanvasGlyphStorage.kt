package com.tober.glyphmatrixtools.canvas

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object GlyphCanvasGlyphStorage {
    fun writeGlyphImage(
        context: Context,
        bitmap: Bitmap
    ): String {
        val directory = File(
            context.cacheDir,
            "glyph_canvas_glyph"
        )

        directory.mkdirs()

        directory
            .listFiles()
            ?.forEach { file ->
                try {
                    file.delete()
                } catch (_: Throwable) {
                }
            }

        val file = File(
            directory,
            "glyph_${System.currentTimeMillis()}.png"
        )

        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.flush()
        }

        return file.absolutePath
    }
}
