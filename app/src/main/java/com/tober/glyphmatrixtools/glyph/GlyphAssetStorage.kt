package com.tober.glyphmatrixtools.glyph

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

import com.tober.glyphmatrixtools.canvas.GlyphCanvasAnimationFile

object GlyphAssetStorage {
    fun copyGlyphAssetToTemporaryFile(
        context: Context,
        uri: Uri
    ): Result<GlyphAsset> {
        return runCatching {
            val temporaryRawFile = File(
                context.cacheDir,
                "tmp_glyph_asset_${System.currentTimeMillis()}.tmp"
            )

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(temporaryRawFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                } ?: throw IllegalStateException("Failed to open selected file")

                val type = getGlyphAssetType(temporaryRawFile.absolutePath)
                val extension = getGlyphAssetExtension(type)

                val temporaryAssetFile = File(
                    context.cacheDir,
                    "tmp_glyph_asset_${System.currentTimeMillis()}.$extension"
                )

                temporaryRawFile.copyTo(
                    target = temporaryAssetFile,
                    overwrite = true
                )

                temporaryRawFile.delete()

                GlyphAsset(
                    path = temporaryAssetFile.absolutePath,
                    type = type
                )
            } catch (e: Throwable) {
                temporaryRawFile.delete()
                throw e
            }
        }
    }

    fun saveGlyphAsset(
        context: Context,
        asset: GlyphAsset,
        prefix: String
    ): Result<GlyphAsset> {
        return runCatching {
            val source = File(asset.path)

            require(source.exists()) {
                "Selected glyph asset does not exist"
            }

            validateGlyphFile(
                path = asset.path,
                type = asset.type
            )

            val extension = getGlyphAssetExtension(asset.type)

            val destination = File(
                context.filesDir,
                "${prefix}_${System.currentTimeMillis()}.$extension"
            )

            source.copyTo(
                target = destination,
                overwrite = true
            )

            GlyphAsset(
                path = destination.absolutePath,
                type = asset.type
            )
        }
    }

    private fun validateGlyphFile(
        path: String,
        type: GlyphAssetType
    ) {
        when (type) {
            GlyphAssetType.Image -> {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                BitmapFactory.decodeFile(path, options)

                require(options.outWidth > 0 && options.outHeight > 0) {
                    "Selected file is not a valid image"
                }

                require(options.outWidth == options.outHeight) {
                    "Image must be 1:1 square"
                }
            }

            GlyphAssetType.Animation -> {
                val raw = File(path).readText()

                require(GlyphCanvasAnimationFile.fromJson(raw) != null) {
                    "Selected file is not a valid glyph animation"
                }
            }
        }
    }

    private fun getGlyphAssetType(
        path: String
    ): GlyphAssetType {
        runCatching {
            validateGlyphFile(
                path = path,
                type = GlyphAssetType.Image
            )
        }.onSuccess {
            return GlyphAssetType.Image
        }

        runCatching {
            validateGlyphFile(
                path = path,
                type = GlyphAssetType.Animation
            )
        }.onSuccess {
            return GlyphAssetType.Animation
        }

        throw IllegalStateException("Selected file is not a valid image or glyph animation")
    }

    private fun getGlyphAssetExtension(
        type: GlyphAssetType
    ): String {
        return when (type) {
            GlyphAssetType.Image -> "png"
            GlyphAssetType.Animation -> "gma"
        }
    }

    fun deleteGlyphAsset(
        glyph: Glyph
    ) {
        deleteGlyphFile(glyph.image)
        deleteGlyphFile(glyph.animation)
    }

    fun deleteGlyphAsset(
        asset: GlyphAsset?
    ) {
        deleteGlyphFile(asset?.path)
    }

    fun deleteGlyphFile(
        path: String?
    ) {
        if (path.isNullOrBlank()) return

        GlyphImageCache.remove(path)
        GlyphAnimationCache.remove(path)

        runCatching {
            File(path).delete()
        }
    }
}
