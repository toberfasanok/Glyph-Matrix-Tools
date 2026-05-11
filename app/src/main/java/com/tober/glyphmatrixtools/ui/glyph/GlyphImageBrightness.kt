package com.tober.glyphmatrixtools.ui.glyph

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlin.math.roundToInt

private const val MIN_VISIBLE_GLYPH_IMAGE_BRIGHTNESS = 0.18f

fun visibleGlyphImageBrightness(
    rawBrightness: Float
): Float {
    if (rawBrightness <= 0f) return 0f

    return MIN_VISIBLE_GLYPH_IMAGE_BRIGHTNESS + rawBrightness.coerceIn(0f, 1f) * (1f - MIN_VISIBLE_GLYPH_IMAGE_BRIGHTNESS)
}

fun Bitmap.toVisibleGlyphImageBrightness(): Bitmap {
    val out = createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = this[x, y]
            val alpha = Color.alpha(pixel)

            if (alpha == 0) {
                out[x, y] = 0
                continue
            }

            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            val maxChannel = maxOf(red, green, blue)

            if (maxChannel <= 0) {
                out[x, y] = 0
                continue
            }

            val rawBrightness = maxChannel / 255f
            val visualBrightness = visibleGlyphImageBrightness(rawBrightness)
            val scale = visualBrightness / rawBrightness

            val outputRed = (red * scale)
                .roundToInt()
                .coerceIn(0, 255)

            val outputGreen = (green * scale)
                .roundToInt()
                .coerceIn(0, 255)

            val outputBlue = (blue * scale)
                .roundToInt()
                .coerceIn(0, 255)

            out[x, y] = Color.argb(
                alpha,
                outputRed,
                outputGreen,
                outputBlue
            )
        }
    }

    return out
}
