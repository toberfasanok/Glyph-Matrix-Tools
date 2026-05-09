package com.tober.glyphmatrixtools.glyph

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object GlyphImageStorage {
    fun copyGlyphImageToTemporaryFile(
        context: Context,
        uri: Uri
    ): Result<String> {
        return runCatching {
            val newFile = File(
                context.filesDir,
                "tmp_image_${System.currentTimeMillis()}.png"
            )

            context.filesDir
                .listFiles()
                ?.filter {
                    it.name.startsWith("tmp_image_") &&
                        it.name.endsWith(".png") &&
                        it.absolutePath != newFile.absolutePath
                }
                ?.forEach {
                    runCatching {
                        it.delete()
                    }
                }

            context.contentResolver.openInputStream(uri).use { inputStream ->
                requireNotNull(inputStream) {
                    "Failed to open selected image"
                }

                FileOutputStream(newFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
            }

            requireSquareImage(newFile)

            newFile.absolutePath
        }
    }

    fun saveGlyphImage(
        context: Context,
        path: String,
        prefix: String = "glyph"
    ): Result<String> {
        return runCatching {
            val source = File(path)

            require(source.exists()) {
                "Selected glyph does not exist"
            }

            val destination = File(
                context.filesDir,
                "${prefix}_${System.currentTimeMillis()}.png"
            )

            source.copyTo(destination, overwrite = true)

            destination.absolutePath
        }
    }

    fun deleteGlyphImage(
        path: String?
    ) {
        if (path.isNullOrBlank()) return

        GlyphImageCache.remove(path)

        runCatching {
            File(path).delete()
        }
    }

    private fun requireSquareImage(
        file: File
    ) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(file.absolutePath, options)

        require(options.outWidth > 0 && options.outHeight > 0) {
            "Failed to read selected image"
        }

        require(options.outWidth == options.outHeight) {
            "Image must have 1:1 square ratio"
        }
    }
}
