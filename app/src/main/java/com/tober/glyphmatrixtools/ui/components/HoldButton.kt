package com.tober.glyphmatrixtools.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HoldButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    shouldKeepRepeating: () -> Boolean = { true },

    initialDelay: Long = 500L,
    repeatDelay: Long = 50L,

    onClick: () -> Unit,
    onRepeat: () -> Unit = onClick
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnRepeat by rememberUpdatedState(onRepeat)
    val currentShouldKeepRepeating by rememberUpdatedState(shouldKeepRepeating)
    val enabledState = rememberUpdatedState(enabled)

    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember {
        MutableInteractionSource()
    }

    var isPressed by remember {
        mutableStateOf(false)
    }

    var repeatedDuringPress by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(isPressed) {
        if (!isPressed) return@LaunchedEffect

        delay(initialDelay)

        while (
            isPressed &&
            enabledState.value &&
            currentShouldKeepRepeating()
        ) {
            repeatedDuringPress = true
            currentOnRepeat()

            delay(repeatDelay)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .indication(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 24.dp
                )
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    if (!enabledState.value) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    repeatedDuringPress = false
                    isPressed = true

                    val press = PressInteraction.Press(down.position)

                    coroutineScope.launch {
                        interactionSource.emit(press)
                    }

                    try {
                        val up = waitForUpOrCancellation()

                        if (up == null) {
                            coroutineScope.launch {
                                interactionSource.emit(
                                    PressInteraction.Cancel(press)
                                )
                            }
                        } else {
                            coroutineScope.launch {
                                interactionSource.emit(
                                    PressInteraction.Release(press)
                                )
                            }

                            if (
                                !repeatedDuringPress &&
                                enabledState.value &&
                                currentShouldKeepRepeating()
                            ) {
                                currentOnClick()
                            }
                        }
                    } finally {
                        isPressed = false
                        repeatedDuringPress = false
                    }
                }
            }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        }
    }
}
