package com.tober.glyphmatrixtools.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import com.tober.glyphmatrixtools.ui.fields.NumberField
import com.tober.glyphmatrixtools.ui.screens.Screen
import com.tober.glyphmatrixtools.ui.screens.ScreenSpacer
import com.tober.glyphmatrixtools.util.Constants
import com.tober.glyphmatrixtools.util.ToastService

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val toastService = remember {
        ToastService(context)
    }

    Screen(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            NumberField(
                title = "Screen Wake Glyph Timeout",
                label = "in seconds",

                preferenceKey = Constants.PREFERENCES_SCREEN_WAKE_GLYPH_TIMEOUT,
                defaultValue = 5L,

                onSaved = {
                    toastService.show("Screen Wake Glyph timeout saved")
                },
                onReset = {
                    toastService.show("Screen Wake Glyph timeout reset")
                }
            )
        }

        item { ScreenSpacer() }

        item {
            NumberField(
                title = "Notification Glyph Timeout",
                label = "in seconds",

                preferenceKey = Constants.PREFERENCES_NOTIFICATION_GLYPH_TIMEOUT,
                defaultValue = 5L,

                onSaved = {
                    toastService.show("Notification Glyph timeout saved")
                },
                onReset = {
                    toastService.show("Notification Glyph timeout reset")
                }
            )
        }

        item { ScreenSpacer() }

        item {
            NumberField(
                title = "Circle Animation Speed",
                label = "in milliseconds",

                preferenceKey = Constants.PREFERENCES_CIRCLE_ANIMATION_SPEED,
                defaultValue = 25L,

                onSaved = {
                    toastService.show("Circle animation speed saved")
                },
                onReset = {
                    toastService.show("Circle animation speed reset")
                }
            )
        }
    }
}
