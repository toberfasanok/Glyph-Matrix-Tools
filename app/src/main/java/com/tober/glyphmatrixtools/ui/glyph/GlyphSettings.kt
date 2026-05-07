package com.tober.glyphmatrixtools.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.tober.glyphmatrixtools.glyph.Glyph

@Composable
fun GlyphSettings(
    glyph: Glyph,

    useImage: Boolean = true,

    onChangeGlyphSettings: (Glyph) -> Unit,
    onDeleteGlyph: (Glyph) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Settings")
        },
        text = {
            if (useImage) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Circle Animation")

                        Text(
                            text = "The Glyph will be animated as an expanding / shrinking circle when appearing / disappearing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Switch(
                        checked = glyph.imageAnimate,
                        onCheckedChange = { imageAnimate ->
                            onChangeGlyphSettings(
                                glyph.copy(imageAnimate = imageAnimate)
                            )
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDeleteGlyph(glyph)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = "Done")
            }
        }
    )
}
