package com.tober.glyphmatrixtools.ui.glyph

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

import com.tober.glyphmatrixtools.glyph.GlyphImageStorage

@Composable
fun glyphImagePicker(
    onGlyphImagePicked: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val currentOnError = rememberUpdatedState(onError)
    val currentOnGlyphImagePicked = rememberUpdatedState(onGlyphImagePicked)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        GlyphImageStorage
            .copyGlyphImageToTemporaryFile(context, uri)
            .onSuccess { path ->
                currentOnGlyphImagePicked.value(path)
            }
            .onFailure { error ->
                currentOnError.value(error.message ?: "Failed to load image")
            }
    }

    return remember(launcher) {
        {
            launcher.launch(arrayOf("image/*"))
        }
    }
}
