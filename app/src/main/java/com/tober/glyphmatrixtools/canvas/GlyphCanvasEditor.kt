package com.tober.glyphmatrixtools.canvas

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set

class GlyphCanvasEditor(
    val matrixSize: Int = GlyphCanvasMask.MATRIX_SIZE,
    val mask: List<Boolean> = GlyphCanvasMask.values
) {
    fun cellIndexAt(
        x: Float,
        y: Float,
        cellSizePx: Float,
        cellSpacingPx: Float
    ): Int? {
        if (x < 0f || y < 0f) return null

        val step = cellSizePx + cellSpacingPx
        val column = (x / step).toInt()
        val row = (y / step).toInt()

        if (row !in 0 until matrixSize) return null
        if (column !in 0 until matrixSize) return null

        val localX = x - column * step
        val localY = y - row * step

        if (localX > cellSizePx || localY > cellSizePx) return null

        val index = row * matrixSize + column

        if (!mask[index]) return null

        return index
    }

    fun setCell(
        cells: MutableList<Int>,
        index: Int,
        brightness: Int
    ): Boolean {
        if (index !in cells.indices) return false
        if (!mask[index]) return false

        val value = brightness.coerceIn(0, 100)

        if (cells[index] == value) return false

        cells[index] = value

        return true
    }

    fun clear(
        cells: MutableList<Int>
    ) {
        for (index in cells.indices) {
            cells[index] = 0
        }
    }

    fun reverse(
        cells: MutableList<Int>
    ) {
        for (index in cells.indices) {
            if (mask[index]) {
                cells[index] = 100 - cells[index].coerceIn(0, 100)
            } else {
                cells[index] = 0
            }
        }
    }

    fun move(
        cells: MutableList<Int>,
        dx: Int,
        dy: Int
    ) {
        val moved = IntArray(matrixSize * matrixSize)

        for (row in 0 until matrixSize) {
            for (column in 0 until matrixSize) {
                val index = row * matrixSize + column

                if (!mask[index]) continue

                val value = cells[index].coerceIn(0, 100)
                if (value == 0) continue

                val newRow = row + dy
                val newColumn = column + dx

                if (newRow !in 0 until matrixSize) continue
                if (newColumn !in 0 until matrixSize) continue

                val newIndex = newRow * matrixSize + newColumn

                if (mask[newIndex]) {
                    moved[newIndex] = value
                }
            }
        }

        for (index in cells.indices) {
            cells[index] = if (mask[index]) {
                moved[index].coerceIn(0, 100)
            } else {
                0
            }
        }
    }

    fun toBitmap(
        cells: List<Int>
    ): Bitmap {
        val bitmap = createBitmap(matrixSize, matrixSize, Bitmap.Config.ARGB_8888)

        for (row in 0 until matrixSize) {
            for (column in 0 until matrixSize) {
                val index = row * matrixSize + column

                val brightness = if (mask[index]) {
                    cells[index].coerceIn(0, 100)
                } else {
                    0
                }

                val channel = ((brightness / 100f) * 255f)
                    .toInt()
                    .coerceIn(0, 255)

                bitmap[column, row] = if (channel == 0) {
                    0x00000000
                } else {
                    android.graphics.Color.argb(
                        255,
                        channel,
                        channel,
                        channel
                    )
                }
            }
        }

        return bitmap
    }

    fun loadBitmap(
        cells: MutableList<Int>,
        bitmap: Bitmap
    ) {
        val scaled = bitmap.scale(matrixSize, matrixSize)

        for (row in 0 until matrixSize) {
            for (column in 0 until matrixSize) {
                val index = row * matrixSize + column

                if (!mask[index]) {
                    cells[index] = 0
                    continue
                }

                val pixel = scaled[column, row]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                val luminance = 0.299 * red + 0.587 * green + 0.114 * blue

                cells[index] = ((luminance / 255.0) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
            }
        }
    }
}
