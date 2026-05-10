package com.tober.glyphmatrixtools.ui.screens.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext

import com.tober.glyphmatrixtools.canvas.GlyphCanvasConstants
import com.tober.glyphmatrixtools.canvas.GlyphCanvasDraft
import com.tober.glyphmatrixtools.canvas.GlyphCanvasEditor
import com.tober.glyphmatrixtools.canvas.GlyphCanvasMask
import com.tober.glyphmatrixtools.canvas.GlyphCanvasDraftStorage

enum class GlyphCanvasPaintMode {
    Paint,
    Erase
}

class GlyphCanvasState(
    val editor: GlyphCanvasEditor,
    private val storage: GlyphCanvasDraftStorage,
    val cells: SnapshotStateList<Int>
) {
    var paintMode by mutableStateOf(GlyphCanvasPaintMode.Paint)

    var brushBrightness by mutableIntStateOf(GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS)

    private val undoHistory = mutableStateListOf<List<Int>>()
    private val redoHistory = mutableStateListOf<List<Int>>()

    private var editStartSnapshot: List<Int>? = null

    fun loadDraft() {
        val draft = storage.getDraft()

        cells.clear()
        cells.addAll(draft.toCells(editor.mask))

        brushBrightness = draft.brushBrightness.coerceIn(0, 100)

        undoHistory.clear()
        undoHistory.addAll(draft.undoHistory.takeLast(GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS))

        redoHistory.clear()
        redoHistory.addAll(draft.redoHistory.takeLast(GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS))
    }

    fun persist() {
        storage.setDraft(
            GlyphCanvasDraft.fromCells(
                cells = cells,
                matrixSize = editor.matrixSize,
                undoHistory = undoHistory,
                redoHistory = redoHistory,
                brushBrightness = brushBrightness
            )
        )
    }

    fun paintCell(
        index: Int
    ): Boolean {
        val brightness = when (paintMode) {
            GlyphCanvasPaintMode.Paint -> brushBrightness
            GlyphCanvasPaintMode.Erase -> 0
        }

        return editor.setCell(
            cells = cells,
            index = index,
            brightness = brightness
        )
    }

    fun beginEdit() {
        editStartSnapshot = snapshot()
    }

    fun finishEdit(
        changed: Boolean
    ) {
        val before = editStartSnapshot

        editStartSnapshot = null

        if (changed && before != null) {
            pushUndoSnapshot(before)
            persist()
        }
    }

    fun clear() {
        val before = snapshot()

        editor.clear(cells)

        pushUndoSnapshot(before)
        persist()
    }

    fun reverse() {
        val before = snapshot()

        editor.reverse(cells)

        pushUndoSnapshot(before)
        persist()
    }

    fun move(
        dx: Int,
        dy: Int
    ) {
        val before = snapshot()

        editor.move(
            cells = cells,
            dx = dx,
            dy = dy
        )

        pushUndoSnapshot(before)
        persist()
    }

    fun createBitmap(): Bitmap {
        return editor.toBitmap(cells)
    }

    fun loadBitmap(
        bitmap: Bitmap
    ) {
        val before = snapshot()

        editor.loadBitmap(
            cells = cells,
            bitmap = bitmap
        )

        pushUndoSnapshot(before)
        persist()
    }

    fun undo() {
        if (undoHistory.isEmpty()) return

        val current = snapshot()
        val previous = undoHistory.removeAt(undoHistory.lastIndex)

        redoHistory.add(current)

        while (redoHistory.size > GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS) {
            redoHistory.removeAt(0)
        }

        restoreSnapshot(previous)
        persist()
    }

    fun redo() {
        if (redoHistory.isEmpty()) return

        val current = snapshot()
        val next = redoHistory.removeAt(redoHistory.lastIndex)

        undoHistory.add(current)

        while (undoHistory.size > GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS) {
            undoHistory.removeAt(0)
        }

        restoreSnapshot(next)
        persist()
    }

    val canUndo: Boolean
        get() = undoHistory.isNotEmpty()

    val canRedo: Boolean
        get() = redoHistory.isNotEmpty()

    private fun snapshot(): List<Int> {
        return cells.toList()
    }

    private fun restoreSnapshot(
        snapshot: List<Int>
    ) {
        cells.clear()
        cells.addAll(
            snapshot.mapIndexed { index, value ->
                if (editor.mask[index]) {
                    value.coerceIn(0, 100)
                } else {
                    0
                }
            }
        )
    }

    private fun pushUndoSnapshot(
        before: List<Int>
    ) {
        if (before == snapshot()) return

        undoHistory.add(before)

        while (undoHistory.size > GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS) {
            undoHistory.removeAt(0)
        }

        redoHistory.clear()
    }
}

@Composable
fun rememberGlyphCanvasState(): GlyphCanvasState {
    val context = LocalContext.current

    val editor = remember {
        GlyphCanvasEditor()
    }

    val storage = remember {
        GlyphCanvasDraftStorage(context.applicationContext)
    }

    val cells = remember {
        mutableStateListOf<Int>().apply {
            repeat(GlyphCanvasMask.MATRIX_SIZE * GlyphCanvasMask.MATRIX_SIZE) {
                add(0)
            }
        }
    }

    val state = remember {
        GlyphCanvasState(
            editor = editor,
            storage = storage,
            cells = cells
        )
    }

    LaunchedEffect(Unit) {
        state.loadDraft()
    }

    return state
}
