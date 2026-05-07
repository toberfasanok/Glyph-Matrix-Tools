package com.tober.glyphmatrixtools.events.call

import android.content.Context

import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.glyph.GlyphStorage
import com.tober.glyphmatrixtools.util.Constants

class CallGlyphsService(
    context: Context
) {
    private val defaultGlyphStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_CALL_DEFAULT_GLYPH
    )

    private val contactGlyphsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_CALL_CONTACT_GLYPHS
    )

    private val ignoredContactsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_CALL_IGNORED_CONTACTS
    )

    fun resolveGlyph(
        number: String,
        contact: String
    ): Glyph? {
        val normalizedNumber = number.trim()
        val normalizedContact = contact.trim()

        val ignored = ignoredContactsStorage
            .getGlyphs()
            .any { glyph ->
                normalizedNumber.contains(glyph.contact.orEmpty(), ignoreCase = true) || normalizedContact.contains(glyph.contact.orEmpty(), ignoreCase = true)
            }

        if (ignored) {
            return null
        }

        val contactGlyph = contactGlyphsStorage
            .getGlyphs()
            .firstOrNull { glyph ->
                normalizedNumber.contains(glyph.contact.orEmpty(), ignoreCase = true) || normalizedContact.contains(glyph.contact.orEmpty(), ignoreCase = true)
            }

        if (contactGlyph != null) {
            return contactGlyph
        }

        val defaultGlyph = defaultGlyphStorage.getGlyph()

        if (defaultGlyph != null) {
            return defaultGlyph
        }

        return null
    }
}
