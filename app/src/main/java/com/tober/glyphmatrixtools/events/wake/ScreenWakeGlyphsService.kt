package com.tober.glyphmatrixtools.events.wake

import android.content.Context
import kotlin.random.Random

import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.glyph.GlyphStorage
import com.tober.glyphmatrixtools.util.Constants

class ScreenWakeGlyphsService(
    context: Context
) {
    private val glyphsStorage = GlyphStorage(context.applicationContext, Constants.PREFERENCES_SCREEN_WAKE_GLYPHS)

    fun resolveGlyph(): Glyph? {
        val glyphs = glyphsStorage.getGlyphs()

        if (glyphs.isEmpty()) return null

        return glyphs[Random.nextInt(glyphs.size)]
    }
}
