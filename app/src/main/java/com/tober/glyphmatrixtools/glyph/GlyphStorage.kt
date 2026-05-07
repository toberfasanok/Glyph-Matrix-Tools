package com.tober.glyphmatrixtools.glyph

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

import com.tober.glyphmatrixtools.util.PreferencesService

class GlyphStorage(
    context: Context,
    private val preferenceKey: String
) {
    private val preferencesService = PreferencesService(context)

    fun getGlyphs(): List<Glyph> {
        val raw = preferencesService.getString(preferenceKey) ?: return emptyList()

        val arr = JSONArray(raw)
        val list = mutableListOf<Glyph>()

        for (i in 0 until arr.length()) {
            list.add(arr.getJSONObject(i).toGlyph())
        }

        return list
            .sortedBy { it.order }
            .mapIndexed { index, item ->
                item.copy(order = index)
            }
    }

    fun setGlyphs(
        glyphs: List<Glyph>
    ) {
        val arr = JSONArray()

        glyphs
            .mapIndexed { index, item ->
                item.copy(order = index)
            }
            .forEach { item ->
                arr.put(item.toJson())
            }

        preferencesService.setString(preferenceKey, arr.toString())
    }

    fun getGlyph(): Glyph? {
        val raw = preferencesService.getString(preferenceKey) ?: return null

        return JSONObject(raw).toGlyph()
    }

    fun setGlyph(
        glyph: Glyph
    ) {
        preferencesService.setString(
            preferenceKey,
            glyph.copy(order = 0).toJson().toString()
        )
    }

    fun removeGlyph() {
        preferencesService.remove(preferenceKey)
    }

    private fun JSONObject.toGlyph(): Glyph {
        return Glyph(
            id = optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            order = optInt("order", 0),

            image = optString("image").takeIf { it.isNotBlank() },
            imageAnimate = optBoolean("imageAnimate", true),

            appLabel = optString("appLabel").takeIf { it.isNotBlank() },
            appPackageName = optString("appPackageName").takeIf { it.isNotBlank() },

            contact = optString("contact").takeIf { it.isNotBlank() }
        )
    }

    private fun Glyph.toJson(): JSONObject {
        val obj = JSONObject()

        obj.put("id", id)
        obj.put("order", order)

        if (!image.isNullOrBlank()) {
            obj.put("image", image)
        }
        obj.put("imageAnimate", imageAnimate)

        if (!appLabel.isNullOrBlank()) {
            obj.put("appLabel", appLabel)
        }
        if (!appPackageName.isNullOrBlank()) {
            obj.put("appPackageName", appPackageName)
        }

        if (!contact.isNullOrBlank()) {
            obj.put("contact", contact)
        }

        return obj
    }
}
