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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay

import com.tober.glyphmatrixtools.glyph.GlyphAsset
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
    animation: String? = null,
    imageSize: Dp = 56.dp,
    onGlyphAssetPicked: (GlyphAsset) -> Unit = {},
    onGlyphAssetPickError: (String) -> Unit = {},

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
    val onClickGlyphAsset = glyphAssetPicker(
        onGlyphAssetPicked = onGlyphAssetPicked,
        onError = onGlyphAssetPickError
    )

    val density = LocalDensity.current
    val imageSizePx = remember(imageSize, density) {
        with(density) {
            imageSize.roundToPx()
        }
    }

    val assetPreview = remember(image, animation, imageSizePx) {
        loadGlyphAsset(
            image = image,
            animation = animation,
            targetSize = imageSizePx
        )
    }

    val animationAssetPreview = assetPreview as? GlyphAssetPreview.Animation

    var animationPreviewFrameIndex by remember(animation, animationAssetPreview?.frames?.size) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(
        animation,
        animationAssetPreview?.frameTime,
        animationAssetPreview?.frames?.size
    ) {
        val animationPreview = animationAssetPreview ?: return@LaunchedEffect
        val frameCount = animationPreview.frames.size

        if (frameCount <= 1) {
            animationPreviewFrameIndex = 0
            return@LaunchedEffect
        }

        animationPreviewFrameIndex = 0

        while (true) {
            delay(animationPreview.frameTime.toLong().coerceAtLeast(1L))

            animationPreviewFrameIndex =
                (animationPreviewFrameIndex + 1) % frameCount
        }
    }

    val preview = when (val currentPreview = assetPreview) {
        is GlyphAssetPreview.Image -> {
            currentPreview.image
        }

        is GlyphAssetPreview.Animation -> {
            currentPreview.frames.getOrNull(
                animationPreviewFrameIndex.coerceIn(
                    0,
                    currentPreview.frames.lastIndex
                )
            )
        }

        null -> {
            null
        }
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
                if (preview != null) {
                    Image(
                        painter = BitmapPainter(
                            image = preview.asImageBitmap(),
                            filterQuality = FilterQuality.None
                        ),
                        contentDescription = "Glyph preview",
                        modifier = Modifier
                            .size(imageSize)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onClickGlyphAsset()
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
                                onClickGlyphAsset()
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
