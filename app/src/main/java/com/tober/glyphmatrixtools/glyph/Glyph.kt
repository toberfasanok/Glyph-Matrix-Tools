package com.tober.glyphmatrixtools.glyph

import java.util.UUID

data class Glyph(
    val id: String = UUID.randomUUID().toString(),
    val order: Int,

    val image: String? = null,
    val animation: String? = null,
    val circleAnimate: Boolean = true,

    val appLabel: String? = null,
    val appPackageName: String? = null,

    val contact: String? = null
)
