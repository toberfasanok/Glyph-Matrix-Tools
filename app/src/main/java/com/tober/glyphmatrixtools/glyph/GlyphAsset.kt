package com.tober.glyphmatrixtools.glyph

enum class GlyphAssetType {
    Image,
    Animation
}

data class GlyphAsset(
    val path: String,
    val type: GlyphAssetType
)
