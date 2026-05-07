package com.tober.glyphmatrixtools.ui.screens.wake

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.tober.glyphmatrixtools.ui.fields.SwitchField
import com.tober.glyphmatrixtools.ui.glyph.glyphItemList
import com.tober.glyphmatrixtools.ui.glyph.rememberGlyphItemListState
import com.tober.glyphmatrixtools.ui.screens.Screen
import com.tober.glyphmatrixtools.ui.screens.ScreenSpacer
import com.tober.glyphmatrixtools.util.Constants

@Composable
fun ScreenWakeGlyphsScreen(
    modifier: Modifier = Modifier
) {
    val glyphsState = rememberGlyphItemListState(
        title = "Glyphs",

        preferenceKey = Constants.PREFERENCES_SCREEN_WAKE_GLYPHS,

        useImage = true,
        imagePrefix = "screen_wake_glyph",

        useApp = false,

        useContact = false
    )

    Screen(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            SwitchField(
                title = "Active",

                preferenceKey = Constants.PREFERENCES_SCREEN_WAKE_GLYPHS_ACTIVE
            )
        }

        item { ScreenSpacer() }

        glyphItemList(
            state = glyphsState
        )
    }
}
