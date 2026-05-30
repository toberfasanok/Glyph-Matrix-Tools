package com.tober.glyphmatrixtools.canvas

import org.json.JSONArray
import org.json.JSONObject

data class GlyphCanvasSnapshot(
    val frames: List<List<Int>>,
    val currentFrameIndex: Int,
    val brushBrightness: Int,
    val animationFrameTime: Int
)

data class GlyphCanvasDraft(
    val matrixSize: Int,

    val frames: List<List<Int>>,
    val currentFrameIndex: Int = 0,
    val brushBrightness: Int = GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS,
    val animationFrameTime: Int = GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME

    // val undoHistory: List<GlyphCanvasSnapshot> = emptyList(),
    // val redoHistory: List<GlyphCanvasSnapshot> = emptyList()
) {
    fun toJson(): String {
        val obj = JSONObject()

        obj.put("matrixSize", matrixSize)

        obj.put("frames", frames.toFramesJsonArray())
        obj.put("currentFrameIndex", currentFrameIndex)
        obj.put("brushBrightness", brushBrightness.coerceIn(1, 100))
        obj.put("animationFrameTime", animationFrameTime.coerceAtLeast(1))

        // obj.put("undoHistory", undoHistory.toSnapshotsJsonArray())
        // obj.put("redoHistory", redoHistory.toSnapshotsJsonArray())

        return obj.toString()
    }

    fun safeFrames(
        mask: List<Boolean>
    ): List<List<Int>> {
        val total = matrixSize * matrixSize

        return frames
            .ifEmpty {
                listOf(List(total) { 0 })
            }
            .map { frame ->
                frame
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
    }

    companion object {
        fun empty(
            matrixSize: Int = GlyphCanvasMask.MATRIX_SIZE
        ): GlyphCanvasDraft {
            return GlyphCanvasDraft(
                matrixSize = matrixSize,
                frames = listOf(List(matrixSize * matrixSize) { 0 })
            )
        }

        fun fromState(
            matrixSize: Int,

            frames: List<List<Int>>,
            currentFrameIndex: Int,
            brushBrightness: Int,
            animationFrameTime: Int

            // undoHistory: List<GlyphCanvasSnapshot>,
            // redoHistory: List<GlyphCanvasSnapshot>
        ): GlyphCanvasDraft {
            return GlyphCanvasDraft(
                matrixSize = matrixSize,

                frames = frames.map { frame -> frame.map { it.coerceIn(0, 100) } },
                currentFrameIndex = currentFrameIndex.coerceAtLeast(0),
                brushBrightness = brushBrightness.coerceIn(1, 100),
                animationFrameTime = animationFrameTime.coerceAtLeast(1)

                // undoHistory = undoHistory,
                // redoHistory = redoHistory
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

                val frames = when {
                    obj.has("frames") -> {
                        obj.optJSONArray("frames").toFrames(total)
                    }

                    else -> {
                        listOf(List(total) { 0 })
                    }
                }

                GlyphCanvasDraft(
                    matrixSize = matrixSize,

                    frames = frames.ifEmpty { listOf(List(total) { 0 }) },
                    currentFrameIndex = obj.optInt("currentFrameIndex", 0),
                    brushBrightness = obj.optInt(
                        "brushBrightness",
                        GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS
                    ).coerceIn(1, 100),
                    animationFrameTime = obj.optInt(
                        "animationFrameTime",
                        GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME
                    ).coerceAtLeast(1)

                    // undoHistory = obj.optJSONArray("undoHistory").toSnapshots(),
                    // redoHistory = obj.optJSONArray("redoHistory").toSnapshots()
                )
            } catch (_: Throwable) {
                empty()
            }
        }
    }
}

private fun JSONArray?.toFrames(
    total: Int
): List<List<Int>> {
    if (this == null) return emptyList()

    return List(length()) { frameIndex ->
        val frameArray = optJSONArray(frameIndex)

        if (frameArray == null) {
            List(total) { 0 }
        } else {
            List(total) { cellIndex ->
                frameArray.optInt(cellIndex, 0).coerceIn(0, 100)
            }
        }
    }
}

// private fun JSONArray?.toSnapshots(): List<GlyphCanvasSnapshot> {
//     if (this == null) return emptyList()

//     return List(length()) { index ->
//         val obj = optJSONObject(index)

//         if (obj == null) {
//             GlyphCanvasSnapshot(
//                 frames = emptyList(),
//                 currentFrameIndex = 0,
//                 brushBrightness = GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS,
//                 animationFrameTime = GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME
//             )
//         } else {
//             val matrixSize = obj.optInt("matrixSize", GlyphCanvasMask.MATRIX_SIZE)
//             val total = matrixSize * matrixSize

//             GlyphCanvasSnapshot(
//                 frames = obj.optJSONArray("frames").toFrames(total),
//                 currentFrameIndex = obj.optInt("currentFrameIndex", 0),
//                 brushBrightness = obj.optInt(
//                     "brushBrightness",
//                     GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS
//                 ).coerceIn(1, 100),
//                 animationFrameTime = obj.optInt(
//                     "animationFrameTime",
//                     GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME
//                 ).coerceAtLeast(1)
//             )
//         }
//     }
// }

private fun List<List<Int>>.toFramesJsonArray(): JSONArray {
    val arr = JSONArray()

    forEach { frame ->
        val frameArray = JSONArray()

        frame.forEach {
            frameArray.put(it.coerceIn(0, 100))
        }

        arr.put(frameArray)
    }

    return arr
}

// private fun List<GlyphCanvasSnapshot>.toSnapshotsJsonArray(): JSONArray {
//     val arr = JSONArray()

//     forEach { snapshot ->
//         val obj = JSONObject()

//         obj.put("matrixSize", GlyphCanvasMask.MATRIX_SIZE)

//         obj.put("frames", snapshot.frames.toFramesJsonArray())
//         obj.put("currentFrameIndex", snapshot.currentFrameIndex)
//         obj.put("brushBrightness", snapshot.brushBrightness.coerceIn(1, 100))
//         obj.put("animationFrameTime", snapshot.animationFrameTime.coerceAtLeast(1))

//         arr.put(obj)
//     }

//     return arr
// }
