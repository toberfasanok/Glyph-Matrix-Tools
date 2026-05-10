package com.tober.glyphmatrixtools.canvas

import org.json.JSONObject

data class GlyphCanvasDraft(
    val matrixSize: Int,
    val cells: List<Int>,
    val undoHistory: List<List<Int>> = emptyList(),
    val redoHistory: List<List<Int>> = emptyList(),
    val brushBrightness: Int = GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS
) {
    fun toJson(): String {
        val obj = JSONObject()

        obj.put("matrixSize", matrixSize)
        obj.put("cells", cells.toJsonArray())
        obj.put("undoHistory", undoHistory.toHistoryJsonArray())
        obj.put("redoHistory", redoHistory.toHistoryJsonArray())
        obj.put("brushBrightness", brushBrightness.coerceIn(0, 100))

        return obj.toString()
    }

    fun toCells(
        mask: List<Boolean>
    ): List<Int> {
        val total = matrixSize * matrixSize

        return cells
            .take(total)
            .let { values ->
                if (values.size < total) {
                    values + List(total - values.size) { 0 }
                } else {
                    values
                }
            }
            .mapIndexed { index, value ->
                if (mask.getOrNull(index) == true) {
                    value.coerceIn(0, 100)
                } else {
                    0
                }
            }
    }

    private fun List<Int>.toJsonArray(): org.json.JSONArray {
        val arr = org.json.JSONArray()

        forEach {
            arr.put(it.coerceIn(0, 100))
        }

        return arr
    }

    private fun List<List<Int>>.toHistoryJsonArray(): org.json.JSONArray {
        val arr = org.json.JSONArray()

        forEach { snapshot ->
            arr.put(snapshot.toJsonArray())
        }

        return arr
    }

    companion object {
        fun empty(
            matrixSize: Int = GlyphCanvasMask.MATRIX_SIZE
        ): GlyphCanvasDraft {
            return GlyphCanvasDraft(
                matrixSize = matrixSize,
                cells = List(matrixSize * matrixSize) { 0 }
            )
        }

        fun fromCells(
            cells: List<Int>,
            matrixSize: Int = GlyphCanvasMask.MATRIX_SIZE,
            undoHistory: List<List<Int>> = emptyList(),
            redoHistory: List<List<Int>> = emptyList(),
            brushBrightness: Int = GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS
        ): GlyphCanvasDraft {
            return GlyphCanvasDraft(
                matrixSize = matrixSize,
                cells = cells.map { it.coerceIn(0, 100) },
                undoHistory = undoHistory,
                redoHistory = redoHistory,
                brushBrightness = brushBrightness.coerceIn(0, 100)
            )
        }

        fun fromJson(
            raw: String?
        ): GlyphCanvasDraft {
            if (raw.isNullOrBlank()) return empty()

            return try {
                val obj = JSONObject(raw)
                val matrixSize = obj.optInt("matrixSize", GlyphCanvasMask.MATRIX_SIZE)
                val total = matrixSize * matrixSize

                val cells = when (val value = obj.opt("cells")) {
                    is String -> {
                        value
                            .padEnd(total, '0')
                            .take(total)
                            .map {
                                if (it == '1') 100 else 0
                            }
                    }

                    is org.json.JSONArray -> {
                        List(value.length()) { index ->
                            value.optInt(index, 0).coerceIn(0, 100)
                        }
                    }

                    else -> {
                        List(total) { 0 }
                    }
                }

                GlyphCanvasDraft(
                    matrixSize = matrixSize,
                    cells = cells,
                    undoHistory = obj.optJSONArray("undoHistory").toHistory(),
                    redoHistory = obj.optJSONArray("redoHistory").toHistory(),
                    brushBrightness = obj.optInt(
                        "brushBrightness",
                        GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS
                    ).coerceIn(0, 100)
                )
            } catch (_: Throwable) {
                empty()
            }
        }

        private fun org.json.JSONArray?.toHistory(): List<List<Int>> {
            if (this == null) return emptyList()

            return List(length()) { outerIndex ->
                val arr = optJSONArray(outerIndex)

                if (arr == null) {
                    emptyList()
                } else {
                    List(arr.length()) { innerIndex ->
                        arr.optInt(innerIndex, 0).coerceIn(0, 100)
                    }
                }
            }
        }
    }
}
