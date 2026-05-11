package com.tober.glyphmatrixtools.ui.glyph

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.tober.glyphmatrixtools.apps.App
import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.ui.fields.TextField

sealed interface GlyphItemAction {
    data object Save : GlyphItemAction

    data class Settings(
        val glyph: Glyph
    ) : GlyphItemAction
}

@Composable
fun GlyphItem(
    modifier: Modifier = Modifier,

    action: GlyphItemAction,

    useImage: Boolean = true,
    image: String? = null,
    imageSize: Dp = 56.dp,
    onGlyphImagePicked: (String) -> Unit = {},
    onGlyphImagePickError: (String) -> Unit = {},

    useApp: Boolean = false,
    appLabel: String? = null,
    appPackageName: String? = null,
    onGlyphAppPicked: (App) -> Unit = {},

    useContact: Boolean = false,
    contact: String? = null,
    onGlyphContactPicked: (String) -> Unit = {},

    onSaveGlyph: () -> Unit = {},
    onChangeGlyphSettings: (Glyph) -> Unit = {},
    onDeleteGlyph: (Glyph) -> Unit = {}
) {
    // Action
    val actionIcon: ImageVector = when (action) {
        GlyphItemAction.Save -> Icons.Filled.Save
        is GlyphItemAction.Settings -> Icons.Filled.Settings
    }

    val actionContentDescription = when (action) {
        GlyphItemAction.Save -> "Save glyph"
        is GlyphItemAction.Settings -> "Glyph settings"
    }

    val isNewGlyphItem = action is GlyphItemAction.Save

    // Image
    val onClickGlyphImage = glyphImagePicker(
        onGlyphImagePicked = onGlyphImagePicked,
        onError = onGlyphImagePickError
    )

    val density = LocalDensity.current
    val imageSizePx = remember(imageSize, density) {
        with(density) {
            imageSize.roundToPx()
        }
    }

    val bitmap = remember(image, imageSizePx) {
        image
            ?.takeIf { it.isNotBlank() }
            ?.let { loadGlyphImageBitmap(it, imageSizePx) }
            ?.toVisibleGlyphImageBrightness()
    }

    // App
    var showGlyphAppPicker by remember {
        mutableStateOf(false)
    }

    if (useApp && showGlyphAppPicker) {
        GlyphAppPicker(
            onGlyphAppPicked = { app ->
                onGlyphAppPicked(app)
                showGlyphAppPicker = false
            },
            onDismiss = {
                showGlyphAppPicker = false
            }
        )
    }

    // Settings
    var showGlyphSettings by remember {
        mutableStateOf(false)
    }

    val settingsGlyph = (action as? GlyphItemAction.Settings)?.glyph

    if (showGlyphSettings && settingsGlyph != null) {
        GlyphSettings(
            glyph = settingsGlyph,

            useImage = useImage,

            onChangeGlyphSettings = onChangeGlyphSettings,
            onDeleteGlyph = { deletedGlyph ->
                showGlyphSettings = false
                onDeleteGlyph(deletedGlyph)
            },
            onDismiss = {
                showGlyphSettings = false
            }
        )
    }

    // UI
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171717)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (useImage) {
                if (bitmap != null) {
                    Image(
                        painter = BitmapPainter(
                            image = bitmap.asImageBitmap(),
                            filterQuality = FilterQuality.None
                        ),
                        contentDescription = "Glyph preview",
                        modifier = Modifier
                            .size(imageSize)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onClickGlyphImage()
                            }
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(imageSize)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .clickable {
                                onClickGlyphImage()
                            }
                    ) {
                        Text(text = "+")
                    }
                }
            }

            if (useApp) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .clickable {
                            showGlyphAppPicker = true
                        }
                ) {
                    Text(
                        text = appLabel?.takeIf { it.isNotBlank() } ?: "Choose an app",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = appPackageName?.takeIf { it.isNotBlank() } ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (useContact) {
                if (isNewGlyphItem) {
                    TextField(
                        value = contact,

                        label = "Contact",

                        onChangeValue = onGlyphContactPicked,

                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                }
                else {
                    Text(
                        text = contact.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    when (action) {
                        GlyphItemAction.Save -> {
                            if (useContact) {
                                onGlyphContactPicked(contact.orEmpty().trim())
                            }

                            onSaveGlyph()
                        }

                        is GlyphItemAction.Settings -> {
                            showGlyphSettings = true
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionContentDescription
                )
            }
        }
    }
}
