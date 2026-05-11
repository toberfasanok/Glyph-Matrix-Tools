package com.tober.glyphmatrixtools.ui.modifiers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    return this.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    }
}
