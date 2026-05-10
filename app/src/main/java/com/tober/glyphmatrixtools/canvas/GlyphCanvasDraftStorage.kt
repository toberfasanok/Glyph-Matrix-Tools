package com.tober.glyphmatrixtools.canvas

import android.content.Context

import com.tober.glyphmatrixtools.util.Constants
import com.tober.glyphmatrixtools.util.PreferencesService

class GlyphCanvasDraftStorage(
    context: Context
) {
    private val preferencesService = PreferencesService(context.applicationContext)

    fun getDraft(): GlyphCanvasDraft {
        return GlyphCanvasDraft.fromJson(
            preferencesService.getString(Constants.PREFERENCES_GLYPH_CANVAS_DRAFT)
        )
    }

    fun setDraft(
        draft: GlyphCanvasDraft
    ) {
        preferencesService.setString(
            Constants.PREFERENCES_GLYPH_CANVAS_DRAFT,
            draft.toJson()
        )
    }
}
