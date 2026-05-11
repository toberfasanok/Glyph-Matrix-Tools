package com.tober.glyphmatrixtools.ui.glyph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.glyph.GlyphAsset
import com.tober.glyphmatrixtools.glyph.GlyphAssetType
import com.tober.glyphmatrixtools.glyph.GlyphAssetStorage
import com.tober.glyphmatrixtools.glyph.GlyphStorage
import com.tober.glyphmatrixtools.util.ToastService

@Composable
fun DefaultGlyphItem(
    modifier: Modifier = Modifier,

    title: String? = null,

    preferenceKey: String,

    imagePrefix: String,
    imageSize: Dp = 76.dp
) {
    val context = LocalContext.current

    val toastService = remember {
        ToastService(context)
    }

    val glyphStorage = remember(preferenceKey) {
        GlyphStorage(
            context.applicationContext,
            preferenceKey
        )
    }

    var glyph by remember(preferenceKey) {
        mutableStateOf(glyphStorage.getGlyph())
    }

    var newGlyphAsset by remember {
        mutableStateOf<GlyphAsset?>(null)
    }

    val savedGlyph = glyph

    if (title !== null) {
        Text(text = title)

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (savedGlyph == null) {
        GlyphItem(
            action = GlyphItemAction.Save,

            useImage = true,
            image = newGlyphAsset
                ?.takeIf { it.type == GlyphAssetType.Image }
                ?.path,
            animation = newGlyphAsset
                ?.takeIf { it.type == GlyphAssetType.Animation }
                ?.path,
            imageSize = imageSize,
            onGlyphAssetPicked = { temporaryAsset ->
                GlyphAssetStorage.deleteGlyphAsset(newGlyphAsset)
                newGlyphAsset = temporaryAsset
            },
            onGlyphAssetPickError = { error ->
                toastService.show(error)
            },

            onSaveGlyph = {
                val newGlyphAssetCopy = newGlyphAsset

                if (newGlyphAssetCopy == null) {
                    toastService.show("Choose a default glyph")
                    return@GlyphItem
                }

                GlyphAssetStorage
                    .saveGlyphAsset(
                        context = context,
                        asset = newGlyphAssetCopy,
                        prefix = imagePrefix
                    )
                    .onSuccess { savedAsset ->
                        val created = when (savedAsset.type) {
                            GlyphAssetType.Image -> {
                                Glyph(
                                    order = 0,
                                    image = savedAsset.path,
                                    animation = null,
                                    circleAnimate = true
                                )
                            }

                            GlyphAssetType.Animation -> {
                                Glyph(
                                    order = 0,
                                    image = null,
                                    animation = savedAsset.path,
                                    circleAnimate = true
                                )
                            }
                        }

                        glyph = created
                        glyphStorage.setGlyph(created)

                        GlyphAssetStorage.deleteGlyphAsset(newGlyphAsset)
                        newGlyphAsset = null

                        toastService.show("Default glyph saved")
                    }
                    .onFailure { error ->
                        toastService.show(error.message ?: "Failed to save default glyph")
                    }
            },

            modifier = modifier
        )
    } else {
        GlyphItem(
            action = GlyphItemAction.Settings(savedGlyph),

            useImage = true,
            image = savedGlyph.image,
            animation = savedGlyph.animation,
            imageSize = imageSize,
            onGlyphAssetPicked = { temporaryAsset ->
                GlyphAssetStorage
                    .saveGlyphAsset(
                        context = context,
                        asset = temporaryAsset,
                        prefix = imagePrefix
                    )
                    .onSuccess { savedAsset ->
                        val updated = when (savedAsset.type) {
                            GlyphAssetType.Image -> {
                                savedGlyph.copy(
                                    image = savedAsset.path,
                                    animation = null
                                )
                            }

                            GlyphAssetType.Animation -> {
                                savedGlyph.copy(
                                    image = null,
                                    animation = savedAsset.path
                                )
                            }
                        }

                        glyph = updated
                        glyphStorage.setGlyph(updated)

                        GlyphAssetStorage.deleteGlyphAsset(savedGlyph)
                        GlyphAssetStorage.deleteGlyphAsset(temporaryAsset)

                        toastService.show("Default glyph updated")
                    }
                    .onFailure { error ->
                        toastService.show(error.message ?: "Failed to update default glyph")
                    }
            },
            onGlyphAssetPickError = { error ->
                toastService.show(error)
            },

            onChangeGlyphSettings = { updated ->
                glyph = updated
                glyphStorage.setGlyph(updated)
            },
            onDeleteGlyph = { deleted ->
                GlyphAssetStorage.deleteGlyphAsset(deleted)

                glyphStorage.removeGlyph()
                glyph = null

                toastService.show("Default glyph deleted")
            },

            modifier = modifier
        )
    }
}
