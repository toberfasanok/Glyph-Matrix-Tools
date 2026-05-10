package com.tober.glyphmatrixtools.ui.screens.canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.OutputStream
import kotlin.math.roundToInt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.tober.glyphmatrixtools.canvas.GlyphCanvasGlyphStorage
import com.tober.glyphmatrixtools.events.EventService
import com.tober.glyphmatrixtools.util.ToastService

@Composable
fun GlyphCanvasScreen(
    modifier: Modifier = Modifier
) {
    val tag = "Glyph Canvas Screen"

    val context = LocalContext.current

    val state = rememberGlyphCanvasState()
    val toastService = ToastService(context)

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },
            title = {
                Text(text = "Clear Canvas?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.clear()
                        showClearConfirmation = false
                    }
                ) {
                    Text(text = "Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = {
                showSettings = false
            },
            title = {
                Text(text = "Brightness")
            },
            text = {
                Column {
                    Text(text = "${state.brushBrightness}%")

                    Slider(
                        value = state.brushBrightness.toFloat(),
                        onValueChange = { value ->
                            state.brushBrightness = value.roundToInt().coerceIn(0, 100)
                            state.persist()
                        },
                        valueRange = 0f..100f
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettings = false
                    }
                ) {
                    Text(text = "Done")
                }
            }
        )
    }

    val saveImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val bitmap = state.createBitmap()

            context.contentResolver.openOutputStream(uri)?.use { output: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.flush()
            }

            toastService.show("Saved")
        } catch (e: Exception) {
            Log.e(tag, "Failed to save glyph: $e")
            toastService.show("Failed to save glyph")
        }
    }

    val loadImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                    ?: throw Exception("Failed to decode image")

                state.loadBitmap(bitmap)

                toastService.show("Loaded")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load glyph: $e")
            toastService.show("Failed to load glyph")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.persist()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    showClearConfirmation = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Clear"
                )
            }

            Row {
                IconButton(
                    enabled = state.canUndo,
                    onClick = {
                        state.undo()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo"
                    )
                }

                IconButton(
                    enabled = state.canRedo,
                    onClick = {
                        state.redo()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo"
                    )
                }
            }

            IconButton(
                onClick = {
                    try {
                        val bitmap = state.createBitmap()
                        val image = GlyphCanvasGlyphStorage.writeGlyphImage(
                            context = context,
                            bitmap = bitmap
                        )

                        EventService.dispatchGlyphCanvasEvent(
                            context = context,
                            image = image
                        )

                        toastService.show("Displayed")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to display glyph: $e")
                        toastService.show("Failed to display glyph")
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Display"
                )
            }
        }

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val gridSize = minOf(
                maxWidth,
                maxHeight
            ) * 0.98f

            GlyphCanvasGrid(
                state = state,
                gridSize = gridSize
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        saveImageLauncher.launch("glyph_${System.currentTimeMillis()}.png")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save"
                    )
                }

                IconButton(
                    onClick = {
                        loadImageLauncher.launch(arrayOf("image/*"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Load"
                    )
                }

                IconToggleButton(
                    checked = state.paintMode == GlyphCanvasPaintMode.Erase,
                    onCheckedChange = { erase ->
                        state.paintMode = if (erase) {
                            GlyphCanvasPaintMode.Erase
                        } else {
                            GlyphCanvasPaintMode.Paint
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (state.paintMode == GlyphCanvasPaintMode.Erase) {
                            Icons.Filled.CleaningServices
                        } else {
                            Icons.Filled.Brush
                        },
                        contentDescription = if (state.paintMode == GlyphCanvasPaintMode.Erase) {
                            "Erase mode"
                        } else {
                            "Paint mode"
                        }
                    )
                }

                IconButton(
                    onClick = {
                        showSettings = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Canvas settings"
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        state.move(-1, 0)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Move left"
                    )
                }

                IconButton(
                    onClick = {
                        state.move(1, 0)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move right"
                    )
                }

                IconButton(
                    onClick = {
                        state.move(0, -1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move up"
                    )
                }

                IconButton(
                    onClick = {
                        state.move(0, 1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move down"
                    )
                }

                IconButton(
                    onClick = {
                        state.reverse()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Reverse"
                    )
                }
            }
        }
    }
}
