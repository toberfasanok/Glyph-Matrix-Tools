package com.tober.glyphmatrixtools.canvas

import org.json.JSONArray
import org.json.JSONObject

data class GlyphCanvasAnimationFile(
    val matrixSize: Int,
    val frames: List<List<Int>>,
    val frameTime: Int
) {
    fun toJson(): String {
        val obj = JSONObject()

        obj.put("type", TYPE)

        obj.put("matrixSize", matrixSize)
        obj.put("frames", frames.toFramesJsonArray())
        obj.put("frameTime", frameTime.coerceAtLeast(1))

        return obj.toString()
    }

    companion object {
        private const val TYPE = "glyph_matrix_animation"

        fun fromJson(
            raw: String
        ): GlyphCanvasAnimationFile? {
            return try {
                val obj = JSONObject(raw)

                if (obj.optString("type") != TYPE) return null

                val matrixSize = obj.optInt("matrixSize", GlyphCanvasMask.MATRIX_SIZE)
                val frameTime = obj
                    .optInt("frameTime", GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME)
                    .coerceAtLeast(1)

                val framesArray = obj.optJSONArray("frames") ?: return null
                val total = matrixSize * matrixSize

                val frames = List(framesArray.length()) { frameIndex ->
                    val frameArray = framesArray.optJSONArray(frameIndex)

                    if (frameArray == null) {
                        List(total) { 0 }
                    } else {
                        List(total) { cellIndex ->
                            frameArray.optInt(cellIndex, 0).coerceIn(0, 100)
                        }
                    }
                }

                if (frames.isEmpty()) return null

                GlyphCanvasAnimationFile(
                    matrixSize = matrixSize,
                    frameTime = frameTime,
                    frames = frames
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

private fun List<List<Int>>.toFramesJsonArray(): JSONArray {
    val arr = JSONArray()

    forEach { frame ->
        val frameArray = JSONArray()

        frame.forEach { brightness ->
            frameArray.put(brightness.coerceIn(0, 100))
        }

        arr.put(frameArray)
    }

    return arr
}
