package com.tober.glyphmatrixtools.ui.glyph

import android.graphics.Bitmap
import android.graphics.BitmapFactory

import com.tober.glyphmatrixtools.glyph.GlyphImageCache

fun loadGlyphImageBitmap(
    path: String,
    targetSize: Int
): Bitmap? {
    val cacheKey = "$path:$targetSize"

    GlyphImageCache.get(cacheKey)?.let {
        return it
    }

    val bitmap = decodeGlyphImageBitmap(
        path = path,
        targetSize = targetSize
    ) ?: return null

    GlyphImageCache.put(cacheKey, bitmap)

    return bitmap
}

private fun decodeGlyphImageBitmap(
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
