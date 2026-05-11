package com.tober.glyphmatrixtools.ui.glyph

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

import com.tober.glyphmatrixtools.glyph.GlyphAsset
import com.tober.glyphmatrixtools.glyph.GlyphAssetStorage

@Composable
fun glyphAssetPicker(
    onGlyphAssetPicked: (GlyphAsset) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val currentOnError = rememberUpdatedState(onError)
    val currentOnGlyphAssetPicked = rememberUpdatedState(onGlyphAssetPicked)

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

        GlyphAssetStorage
            .copyGlyphAssetToTemporaryFile(context, uri)
            .onSuccess { asset ->
                currentOnGlyphAssetPicked.value(asset)
            }
            .onFailure { error ->
                currentOnError.value(error.message ?: "Failed to load glyph asset")
            }
    }

    return remember(launcher) {
        {
            launcher.launch(
                arrayOf(
                    "image/*",
                    "application/json",
                    "*/*"
                )
            )
        }
    }
}
