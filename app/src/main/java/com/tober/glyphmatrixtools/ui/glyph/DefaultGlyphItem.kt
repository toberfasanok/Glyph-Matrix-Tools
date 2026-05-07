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
import com.tober.glyphmatrixtools.glyph.GlyphImageStorage
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

    var newGlyphImage by remember {
        mutableStateOf<String?>(null)
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
            image = newGlyphImage,
            imageSize = imageSize,
            onGlyphImagePicked = { temporaryImagePath ->
                newGlyphImage = temporaryImagePath
            },
            onGlyphImagePickError = { error ->
                toastService.show(error)
            },

            onSaveGlyph = {
                val newGlyphImageCopy = newGlyphImage

                if (newGlyphImageCopy.isNullOrBlank()) {
                    toastService.show("Choose a default glyph")
                    return@GlyphItem
                }

                GlyphImageStorage
                    .saveGlyphImage(
                        context = context,
                        path = newGlyphImageCopy,
                        prefix = imagePrefix
                    )
                    .onSuccess { savedImage ->
                        val created = Glyph(
                            order = 0,
                            image = savedImage,
                            imageAnimate = true
                        )

                        glyph = created
                        glyphStorage.setGlyph(created)

                        GlyphImageStorage.deleteGlyphImage(newGlyphImageCopy)
                        newGlyphImage = null

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
            imageSize = imageSize,
            onGlyphImagePicked = { temporaryImagePath ->
                GlyphImageStorage
                    .saveGlyphImage(
                        context = context,
                        path = temporaryImagePath,
                        prefix = imagePrefix
                    )
                    .onSuccess { savedImage ->
                        val updated = savedGlyph.copy(
                            image = savedImage
                        )

                        glyph = updated
                        glyphStorage.setGlyph(updated)

                        GlyphImageStorage.deleteGlyphImage(savedGlyph.image)
                        GlyphImageStorage.deleteGlyphImage(temporaryImagePath)

                        toastService.show("Default glyph updated")
                    }
                    .onFailure { error ->
                        toastService.show(error.message ?: "Failed to update default glyph")
                    }
            },
            onGlyphImagePickError = { error ->
                toastService.show(error)
            },

            onChangeGlyphSettings = { updated ->
                glyph = updated
                glyphStorage.setGlyph(updated)
            },
            onDeleteGlyph = { deleted ->
                GlyphImageStorage.deleteGlyphImage(deleted.image)

                glyphStorage.removeGlyph()
                glyph = null

                toastService.show("Default glyph deleted")
            },

            modifier = modifier
        )
    }
}
