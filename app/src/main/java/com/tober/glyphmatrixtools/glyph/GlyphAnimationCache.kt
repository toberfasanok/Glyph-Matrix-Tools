package com.tober.glyphmatrixtools.glyph

import android.graphics.Bitmap
import androidx.collection.LruCache

data class GlyphAnimationCacheEntry(
    val frames: List<Bitmap>,
    val frameTime: Int
)

object GlyphAnimationCache {
    private val cache = LruCache<String, GlyphAnimationCacheEntry>(20)

    fun get(
        key: String
    ): GlyphAnimationCacheEntry? {
        return cache[key]
    }

    fun put(
        key: String,
        value: GlyphAnimationCacheEntry
    ) {
        cache.put(key, value)
    }

    fun remove(
        path: String?
    ) {
        if (path.isNullOrBlank()) return

        val snapshot = cache.snapshot()

        for (key in snapshot.keys) {
            if (key.startsWith("$path:")) {
                cache.remove(key)
            }
        }
    }
}
