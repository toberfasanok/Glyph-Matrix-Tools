package com.tober.glyphmatrixtools.ui.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun GlyphCanvasGrid(
    state: GlyphCanvasState,
    gridSize: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val matrixSize = state.editor.matrixSize
    val cellSpacing = 1.5.dp
    val cellSize = (gridSize - cellSpacing * (matrixSize - 1)) / matrixSize

    val cellSizePx = with(density) {
        cellSize.toPx()
    }

    val cellSpacingPx = with(density) {
        cellSpacing.toPx()
    }

    val onColor = MaterialTheme.colorScheme.primary
    val offColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .size(gridSize)
            .pointerInput(
                cellSizePx,
                cellSpacingPx,
                state.paintMode
            ) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val down = downEvent.changes.firstOrNull {
                            it.pressed && !it.previousPressed
                        } ?: continue

                        val pointerId = down.id
                        var changed = false
                        var lastIndex = -1

                        fun applyPosition(
                            position: Offset
                        ): Boolean {
                            val index = state.editor.cellIndexAt(
                                x = position.x,
                                y = position.y,
                                cellSizePx = cellSizePx,
                                cellSpacingPx = cellSpacingPx
                            ) ?: run {
                                lastIndex = -1
                                return false
                            }

                            if (index == lastIndex) return false

                            lastIndex = index

                            return state.paintCell(index)
                        }

                        state.beginEdit()

                        if (applyPosition(down.position)) {
                            changed = true
                            down.consume()
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull {
                                it.id == pointerId
                            } ?: break

                            if (!change.pressed) break

                            if (applyPosition(change.position)) {
                                changed = true
                                change.consume()
                            }
                        }

                        state.finishEdit(changed)
                    }
                }
            }
    ) {
        for (row in 0 until matrixSize) {
            for (column in 0 until matrixSize) {
                val index = row * matrixSize + column

                if (!state.editor.mask[index]) continue

                val rawBrightness = state.cells[index].coerceIn(0, 100) / 100f
                val left = column * (cellSizePx + cellSpacingPx)
                val top = row * (cellSizePx + cellSpacingPx)
                val corner = cellSizePx * 0.18f
                val strokeWidth = max(1f, cellSizePx * 0.055f)

                val emptyOutlineColor = offColor.copy(alpha = 0.25f)

                if (rawBrightness <= 0f) {
                    drawRoundRect(
                        color = emptyOutlineColor,
                        topLeft = Offset(left, top),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val visualBrightness = 0.18f + rawBrightness * 0.82f

                    val fillColor = androidx.compose.ui.graphics.Color(
                        red = onColor.red * visualBrightness,
                        green = onColor.green * visualBrightness,
                        blue = onColor.blue * visualBrightness,
                        alpha = 1f
                    )

                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(corner, corner)
                    )
                }
            }
        }
    }
}
