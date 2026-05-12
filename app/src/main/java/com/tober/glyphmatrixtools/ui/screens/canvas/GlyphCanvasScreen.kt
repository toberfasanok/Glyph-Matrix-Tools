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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.tober.glyphmatrixtools.canvas.GlyphCanvasAnimationFile
import com.tober.glyphmatrixtools.canvas.GlyphCanvasConstants
import com.tober.glyphmatrixtools.canvas.GlyphCanvasGlyphStorage
import com.tober.glyphmatrixtools.events.EventService
import com.tober.glyphmatrixtools.ui.modifiers.clearFocusOnTap
import com.tober.glyphmatrixtools.util.ToastService

@Composable
fun GlyphCanvasScreen(
    modifier: Modifier = Modifier
) {
    val tag = "Glyph Canvas Screen"

    val context = LocalContext.current

    val toastService = ToastService(context)

    val state = rememberGlyphCanvasState()

    val coroutineScope = rememberCoroutineScope()
    var previewJob by remember {
        mutableStateOf<Job?>(null)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showClear by remember {
        mutableStateOf(false)
    }

    var showCreateFrameDirection by remember {
        mutableStateOf<Int?>(null)
    }

    var showSave by remember {
        mutableStateOf(false)
    }

    var showLoad by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
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

    val saveAnimationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val animationFile = state.createAnimationFile()

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(animationFile.toJson().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            toastService.show("Animation saved")
        } catch (e: Exception) {
            Log.e(tag, "Failed to save animation: $e")
            toastService.show("Failed to save animation")
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

                toastService.show(
                    if (state.isAnimationMode) {
                        "Frame loaded"
                    } else {
                        "Image loaded"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load image: $e")
            toastService.show("Failed to load image")
        }
    }

    val loadAnimationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw Exception("Failed to read animation")

            val animationFile = GlyphCanvasAnimationFile
                .fromJson(rawText)
                ?: throw Exception("Invalid animation file")

            state.loadAnimation(animationFile)

            toastService.show("Animation loaded")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load animation: $e")
            toastService.show("Failed to load animation")
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = {
                showClear = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(text = "Clear")
            },
            text = {
                Column {
                    TextButton(
                        enabled = !state.isPreviewing,
                        onClick = {
                            state.clearCurrentFrame()
                            showClear = false
                        }
                    ) {
                        Text(
                            text = if (state.isAnimationMode) {
                                "Clear Frame"
                            } else {
                                "Clear Canvas"
                            }
                        )
                    }

                    if (state.isAnimationMode) {
                        TextButton(
                            enabled = state.isAnimationMode && !state.isPreviewing,
                            onClick = {
                                state.deleteCurrentFrame()
                                showClear = false
                            }
                        ) {
                            Text(text = "Delete Frame")
                        }

                        TextButton(
                            enabled = state.isAnimationMode && !state.isPreviewing,
                            onClick = {
                                state.clearAnimation()
                                showClear = false
                            }
                        ) {
                            Text(text = "Clear Animation")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showClear = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    showCreateFrameDirection?.let { direction ->
        AlertDialog(
            onDismissRequest = {
                showCreateFrameDirection = null
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(text = "Create Animation Frame")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.createFrame(direction)
                        showCreateFrameDirection = null
                    }
                ) {
                    Text(text = "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFrameDirection = null
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = {
                showSave = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(text = "Save")
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showSave = false
                            saveImageLauncher.launch("glyph_${System.currentTimeMillis()}.png")
                        }
                    ) {
                        Text(
                            text = if (state.isAnimationMode) {
                                "Save Frame"
                            } else {
                                "Save Image"
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            showSave = false
                            saveAnimationLauncher.launch("glyph_animation_${System.currentTimeMillis()}.gma")
                        }
                    ) {
                        Text(text = "Save Animation")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showSave = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showLoad) {
        AlertDialog(
            onDismissRequest = {
                showLoad = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(text = "Load")
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showLoad = false
                            loadImageLauncher.launch(arrayOf("image/*"))
                        }
                    ) {
                        Text(
                            text = if (state.isAnimationMode) {
                                "Load Frame"
                            } else {
                                "Load Image"
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            showLoad = false
                            loadAnimationLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "*/*"
                                )
                            )
                        }
                    ) {
                        Text(text = "Load Animation")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoad = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showSettings) {
        var frameTimeText by remember(showSettings) {
            mutableStateOf(state.animationFrameTime.toString())
        }

        LaunchedEffect(state.animationFrameTime, showSettings) {
            if (showSettings) {
                frameTimeText = state.animationFrameTime.toString()
            }
        }

        fun commitFrameTime() {
            val committed = frameTimeText
                .toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

            frameTimeText = committed.toString()
            state.updateAnimationFrameTime(committed)
        }

        AlertDialog(
            modifier = Modifier.clearFocusOnTap(),
            onDismissRequest = {
                showSettings = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    modifier = Modifier.clearFocusOnTap(),
                    text = "Settings"
                )
            },
            text = {
                Column(
                    modifier = Modifier.clearFocusOnTap()
                ) {
                    Text(
                        text = "Brightness ${state.brushBrightness}%",
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Slider(
                        value = state.brushBrightness.toFloat(),
                        onValueChange = { value ->
                            state.brushBrightness = value.roundToInt().coerceIn(1, 100)
                            state.persist()
                        },
                        valueRange = 1f..100f
                    )

                    if (state.isAnimationMode) {
                        Text(
                            text = "Animation Frame Time",
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = frameTimeText,
                            onValueChange = { value ->
                                frameTimeText = value.filter { it.isDigit() }
                            },
                            label = {
                                Text(text = "milliseconds")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    commitFrameTime()
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.clearFocusOnTap(),
                    onClick = {
                        commitFrameTime()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showSettings = false
                    }
                ) {
                    Text(text = "Done")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            previewJob?.cancel()
            state.persistNow()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    enabled = !state.isPreviewing,
                    onClick = {
                        showClear = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Clear"
                    )
                }

                Row {
                    IconButton(
                        enabled = state.canUndo && !state.isPreviewing,
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
                        enabled = state.canRedo && !state.isPreviewing,
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
                    enabled = true,
                    onClick = {
                        try {
                            if (state.isPreviewing) {
                                previewJob?.cancel()
                                previewJob = null

                                EventService.dispatchGlyphCanvasClear(
                                    context = context
                                )

                                toastService.show("Animation stopped")

                                return@IconButton
                            }

                            if (state.isAnimationMode) {
                                coroutineScope.launch {
                                    previewJob = coroutineScope.launch {
                                        try {
                                            state.previewAnimation(GlyphCanvasConstants.ANIMATION_PREVIEW_TIMEOUT)
                                        } finally {
                                            previewJob = null
                                        }
                                    }
                                }

                                val animationFile = state.createAnimationFile()
                                val animation = GlyphCanvasGlyphStorage.writeGlyphAnimation(
                                    context = context,
                                    animation = animationFile
                                )

                                EventService.dispatchGlyphCanvasEvent(
                                    context = context,
                                    animation = animation
                                )

                                toastService.show("Animation displayed")
                            } else {
                                val bitmap = state.createBitmap()
                                val image = GlyphCanvasGlyphStorage.writeGlyphImage(
                                    context = context,
                                    image = bitmap
                                )

                                EventService.dispatchGlyphCanvasEvent(
                                    context = context,
                                    image = image
                                )

                                toastService.show("Glyph Displayed")
                            }
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

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                IconButton(
                    enabled = !state.isPreviewing,
                    onClick = {
                        val moved = state.goToPreviousFrame()

                        if (!moved) {
                            showCreateFrameDirection = -1
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous frame"
                    )
                }

                if (state.isAnimationMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            enabled = !state.isPreviewing,
                            onClick = {
                                state.copyCurrentFrame()
                                toastService.show("Frame copied")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy frame"
                            )
                        }

                        Text(
                            text = "${state.currentFrameNumber} / ${state.frameCount}",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            enabled = state.canPasteFrame && !state.isPreviewing,
                            onClick = {
                                state.pasteCopiedFrame()
                                toastService.show("Frame pasted")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = "Paste frame"
                            )
                        }
                    }
                }

                IconButton(
                    enabled = !state.isPreviewing,
                    onClick = {
                        val moved = state.goToNextFrame()

                        if (!moved) {
                            showCreateFrameDirection = 1
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next frame"
                    )
                }
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
                    enabled = !state.isPreviewing,
                    onClick = {
                        showSave = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save"
                    )
                }

                IconButton(
                    enabled = !state.isPreviewing,
                    onClick = {
                        showLoad = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Load"
                    )
                }

                IconToggleButton(
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
                    enabled = !state.isPreviewing,
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
