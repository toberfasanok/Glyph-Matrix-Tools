package com.tober.glyphmatrixtools.ui.glyph

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

import com.tober.glyphmatrixtools.canvas.GlyphCanvasAnimationFile
import com.tober.glyphmatrixtools.canvas.GlyphCanvasEditor
import com.tober.glyphmatrixtools.glyph.GlyphAnimationCache
import com.tober.glyphmatrixtools.glyph.GlyphAnimationCacheEntry
import com.tober.glyphmatrixtools.glyph.GlyphImageCache
import com.tober.glyphmatrixtools.glyph.GlyphImageCacheEntry

sealed interface GlyphAssetPreview {
    data class Image(
        val image: Bitmap
    ) : GlyphAssetPreview

    data class Animation(
        val frames: List<Bitmap>,
        val frameTime: Int
    ) : GlyphAssetPreview
}

fun loadGlyphAsset(
    image: String?,
    animation: String?,
    targetSize: Int
): GlyphAssetPreview? {
    return when {
        !animation.isNullOrBlank() -> {
            loadGlyphAnimation(
                path = animation,
                targetSize = targetSize
            )?.let {
                GlyphAssetPreview.Animation(
                    frames = it.frames,
                    frameTime = it.frameTime
                )
            }
        }

        !image.isNullOrBlank() -> {
            loadGlyphImage(
                path = image,
                targetSize = targetSize
            )?.let {
                GlyphAssetPreview.Image(
                    image = it
                )
            }
        }

        else -> null
    }
}

private fun loadGlyphImage(
    path: String,
    targetSize: Int
): Bitmap? {
    val cacheKey = "$path:$targetSize"

    GlyphImageCache.get(cacheKey)?.let {
        return it.image
    }

    val image = decodeGlyphImage(
        path = path,
        targetSize = targetSize
    )?.toVisibleGlyphImageBrightness() ?: return null

    GlyphImageCache.put(
        key = cacheKey,
        value = GlyphImageCacheEntry(
            image = image
        )
    )

    return image
}

private fun decodeGlyphImage(
    path: String,
    targetSize: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeFile(path, bounds)

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1

    while (
        bounds.outWidth / sampleSize > targetSize * 2 ||
        bounds.outHeight / sampleSize > targetSize * 2
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }

    return BitmapFactory.decodeFile(path, options)
}

data class GlyphAnimation(
    val frames: List<Bitmap>,
    val frameTime: Int
)

private fun loadGlyphAnimation(
    path: String,
    targetSize: Int
): GlyphAnimation? {
    val cacheKey = "$path:$targetSize"

    GlyphAnimationCache.get(cacheKey)?.let { cached ->
        return GlyphAnimation(
            frames = cached.frames,
            frameTime = cached.frameTime
        )
    }

    val raw = runCatching {
        File(path).readText()
    }.getOrNull() ?: return null

    val animation = GlyphCanvasAnimationFile.fromJson(raw) ?: return null
    val editor = GlyphCanvasEditor()

    val frames = animation.frames.map { frame ->
        editor
            .toBitmap(frame)
            .toVisibleGlyphImageBrightness()
    }

    if (frames.isEmpty()) return null

    val preview = GlyphAnimation(
        frames = frames,
        frameTime = animation.frameTime.coerceAtLeast(1)
    )

    GlyphAnimationCache.put(
        key = cacheKey,
        value = GlyphAnimationCacheEntry(
            frames = preview.frames,
            frameTime = preview.frameTime
        )
    )

    return preview
}
