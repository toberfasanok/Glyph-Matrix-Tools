package com.tober.glyphmatrixtools.glyph

import android.graphics.Bitmap
import android.util.LruCache

object GlyphImageCache {
    private val cache = LruCache<String, Bitmap>(64)

    fun get(
        key: String
    ): Bitmap? {
        return cache.get(key)
    }

    fun put(
        key: String,
        bitmap: Bitmap
    ) {
        cache.put(key, bitmap)
    }

    fun remove(path: String?) {
        if (path.isNullOrBlank()) return

        val snapshot = cache.snapshot()

        for (key in snapshot.keys) {
            if (key.startsWith("$path:")) {
                cache.remove(key)
            }
        }
    }
}
