package com.tober.glyphmatrixtools.ui.screens.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.tober.glyphmatrixtools.canvas.GlyphCanvasAnimationFile
import com.tober.glyphmatrixtools.canvas.GlyphCanvasConstants
import com.tober.glyphmatrixtools.canvas.GlyphCanvasDraft
import com.tober.glyphmatrixtools.canvas.GlyphCanvasDraftStorage
import com.tober.glyphmatrixtools.canvas.GlyphCanvasEditor
import com.tober.glyphmatrixtools.canvas.GlyphCanvasMask
import com.tober.glyphmatrixtools.canvas.GlyphCanvasSnapshot

enum class GlyphCanvasPaintMode {
    Paint,
    Erase
}

class GlyphCanvasState(
    val editor: GlyphCanvasEditor,
    private val storage: GlyphCanvasDraftStorage,
    private val persistScope: CoroutineScope,
    val cells: SnapshotStateList<Int>
) {
    private val frames = mutableStateListOf<List<Int>>()
    private val undoHistory = mutableStateListOf<GlyphCanvasSnapshot>()
    private val redoHistory = mutableStateListOf<GlyphCanvasSnapshot>()

    private var editStartSnapshot: GlyphCanvasSnapshot? = null

    private var persistJob: Job? = null
    private val persistDebounceMs = 250L

    var paintMode by mutableStateOf(GlyphCanvasPaintMode.Paint)
    var brushBrightness by mutableIntStateOf(GlyphCanvasConstants.DEFAULT_BRUSH_BRIGHTNESS)
    var currentFrameIndex by mutableIntStateOf(0)
        private set
    var animationFrameTime by mutableIntStateOf(GlyphCanvasConstants.DEFAULT_ANIMATION_FRAME_TIME)
    var isPreviewing by mutableStateOf(false)
        private set

    val frameCount: Int
        get() = frames.size.coerceAtLeast(1)

    val currentFrameNumber: Int
        get() = currentFrameIndex + 1

    val isAnimationMode: Boolean
        get() = frameCount > 1

    val canUndo: Boolean
        get() = undoHistory.isNotEmpty()

    val canRedo: Boolean
        get() = redoHistory.isNotEmpty()

    private var copiedFrame: List<Int>? by mutableStateOf(null)
    val canPasteFrame: Boolean
        get() = copiedFrame != null

    fun loadDraft() {
        val draft = storage.getDraft()
        val safeFrames = draft.safeFrames(editor.mask)

        frames.clear()
        frames.addAll(safeFrames)

        currentFrameIndex = draft.currentFrameIndex.coerceIn(0, frames.lastIndex.coerceAtLeast(0))
        restoreFrame(currentFrameIndex)

        brushBrightness = draft.brushBrightness.coerceIn(1, 100)
        animationFrameTime = draft.animationFrameTime.coerceAtLeast(1)

        undoHistory.clear()
        undoHistory.addAll(draft.undoHistory.takeLast(GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS))

        redoHistory.clear()
        redoHistory.addAll(draft.redoHistory.takeLast(GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS))
    }

    fun persist() {
        persistJob?.cancel()

        persistJob = persistScope.launch {
            delay(persistDebounceMs)

            val draft = createDraftSnapshot()

            withContext(Dispatchers.IO) {
                storage.setDraft(draft)
            }
        }
    }

    fun persistNow() {
        persistJob?.cancel()

        persistJob = persistScope.launch {
            val draft = createDraftSnapshot()

            withContext(Dispatchers.IO) {
                storage.setDraft(draft)
            }
        }
    }

    private fun createDraftSnapshot(): GlyphCanvasDraft {
        commitCurrentFrame()

        return GlyphCanvasDraft.fromState(
            matrixSize = editor.matrixSize,

            frames = frames.map { it.toList() },
            currentFrameIndex = currentFrameIndex,
            brushBrightness = brushBrightness,
            animationFrameTime = animationFrameTime,

            undoHistory = undoHistory.toList(),
            redoHistory = redoHistory.toList()
        )
    }

    fun paintCell(
        index: Int
    ): Boolean {
        if (paintMode == GlyphCanvasPaintMode.Paint && brushBrightness <= 0) {
            return false
        }

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
        if (isPreviewing) return
        editStartSnapshot = snapshot()
    }

    fun finishEdit(
        changed: Boolean
    ) {
        val before = editStartSnapshot
        editStartSnapshot = null

        if (changed && before != null) {
            commitCurrentFrame()
            pushUndoSnapshot(before)
            persist()
        }
    }

    fun clearCurrentFrame() {
        if (isPreviewing) return

        val before = snapshot()

        editor.clearFrame(cells)
        commitCurrentFrame()
        pushUndoSnapshot(before)
        persist()
    }

    fun deleteCurrentFrame() {
        if (isPreviewing || frameCount <= 1) return

        val before = snapshot()

        frames.removeAt(currentFrameIndex)

        currentFrameIndex = currentFrameIndex.coerceAtMost(frames.lastIndex)
        restoreFrame(currentFrameIndex)

        pushUndoSnapshot(before)
        persist()
    }

    fun clearAnimation() {
        if (isPreviewing) return

        val before = snapshot()

        frames.clear()
        frames.add(editor.getEmptyFrame())

        currentFrameIndex = 0
        restoreFrame(0)

        pushUndoSnapshot(before)
        persist()
    }

    fun reverse() {
        if (isPreviewing) return

        val before = snapshot()

        editor.reverse(cells)
        commitCurrentFrame()
        pushUndoSnapshot(before)
        persist()
    }

    fun move(
        dx: Int,
        dy: Int
    ) {
        if (isPreviewing) return

        val before = snapshot()

        editor.move(
            cells = cells,
            dx = dx,
            dy = dy
        )

        commitCurrentFrame()
        pushUndoSnapshot(before)
        persist()
    }

    fun goToPreviousFrame(): Boolean {
        if (isPreviewing) return true
        if (currentFrameIndex <= 0) return false

        commitCurrentFrame()
        currentFrameIndex--
        restoreFrame(currentFrameIndex)

        return true
    }

    fun goToNextFrame(): Boolean {
        if (isPreviewing) return true
        if (currentFrameIndex >= frames.lastIndex) return false

        commitCurrentFrame()
        currentFrameIndex++
        restoreFrame(currentFrameIndex)

        return true
    }

    fun createFrame(
        direction: Int
    ) {
        if (isPreviewing) return

        val before = snapshot()
        val insertIndex = if (direction < 0) {
            currentFrameIndex
        } else {
            currentFrameIndex + 1
        }.coerceIn(0, frames.size)

        commitCurrentFrame()
        frames.add(insertIndex, cells.toList())
        currentFrameIndex = insertIndex
        restoreFrame(currentFrameIndex)

        pushUndoSnapshot(before)
        persist()
    }

    fun duplicateAnimationLoop() {
        if (isPreviewing) return
        if (!isAnimationMode) return

        val before = snapshot()

        commitCurrentFrame()

        val copiedFrames = frames.map { frame ->
            frame.toList()
        }

        frames.addAll(copiedFrames)

        pushUndoSnapshot(before)
        persist()
    }

    fun createBitmap(): Bitmap {
        return editor.toBitmap(cells)
    }

    fun createAnimationFile(): GlyphCanvasAnimationFile {
        commitCurrentFrame()

        return GlyphCanvasAnimationFile(
            matrixSize = editor.matrixSize,
            frameTime = animationFrameTime,
            frames = frames.toList()
        )
    }

    fun loadBitmap(
        bitmap: Bitmap
    ) {
        if (isPreviewing) return

        val before = snapshot()

        editor.loadBitmap(
            cells = cells,
            bitmap = bitmap
        )

        commitCurrentFrame()
        pushUndoSnapshot(before)
        persist()
    }

    fun loadAnimation(
        animationFile: GlyphCanvasAnimationFile
    ) {
        if (isPreviewing) return

        val before = snapshot()
        val total = editor.matrixSize * editor.matrixSize

        frames.clear()
        frames.addAll(
            animationFile.frames.map { frame ->
                frame
                    .take(total)
                    .let { values ->
                        if (values.size < total) {
                            values + List(total - values.size) { 0 }
                        } else {
                            values
                        }
                    }
                    .mapIndexed { index, value ->
                        if (editor.mask[index]) {
                            value.coerceIn(0, 100)
                        } else {
                            0
                        }
                    }
            }.ifEmpty {
                listOf(editor.getEmptyFrame())
            }
        )

        currentFrameIndex = 0
        animationFrameTime = animationFile.frameTime.coerceAtLeast(1)
        restoreFrame(0)

        pushUndoSnapshot(before)
        persist()
    }

    fun updateAnimationFrameTime(
        value: Int
    ) {
        if (isPreviewing) return

        val before = snapshot()

        animationFrameTime = value.coerceAtLeast(1)

        pushUndoSnapshot(before)
        persist()
    }

    fun undo() {
        if (isPreviewing || undoHistory.isEmpty()) return

        val previous = undoHistory.removeAt(undoHistory.lastIndex)

        val current = snapshot().copy(
            currentFrameIndex = previous.currentFrameIndex
        )

        redoHistory.add(current)
        trimHistory(redoHistory)

        restoreSnapshot(previous)
        persist()
    }

    fun redo() {
        if (isPreviewing || redoHistory.isEmpty()) return

        val next = redoHistory.removeAt(redoHistory.lastIndex)

        val current = snapshot().copy(
            currentFrameIndex = next.currentFrameIndex
        )

        undoHistory.add(current)
        trimHistory(undoHistory)

        restoreSnapshot(next)
        persist()
    }

    fun copyCurrentFrame() {
        if (isPreviewing) return

        commitCurrentFrame()

        copiedFrame = cells.toList()
    }

    fun pasteCopiedFrame() {
        if (isPreviewing) return

        val frame = copiedFrame ?: return
        val before = snapshot()

        cells.clear()
        cells.addAll(
            frame.mapIndexed { index, value ->
                if (editor.mask[index]) {
                    value.coerceIn(0, 100)
                } else {
                    0
                }
            }
        )

        commitCurrentFrame()
        pushUndoSnapshot(before)
        persist()
    }

    suspend fun previewAnimation(
        timeout: Long
    ) {
        if (!isAnimationMode || isPreviewing) return

        commitCurrentFrame()

        val originalFrameIndex = currentFrameIndex
        val timeoutEnd = android.os.SystemClock.elapsedRealtime() + timeout.coerceAtLeast(1L)

        isPreviewing = true

        try {
            while (android.os.SystemClock.elapsedRealtime() < timeoutEnd) {
                for (index in frames.indices) {
                    if (android.os.SystemClock.elapsedRealtime() >= timeoutEnd) break

                    currentFrameIndex = index
                    restoreFrame(index)

                    val remaining = timeoutEnd - android.os.SystemClock.elapsedRealtime()

                    delay(
                        minOf(
                            animationFrameTime.toLong().coerceAtLeast(1L),
                            remaining.coerceAtLeast(1L)
                        )
                    )
                }
            }
        } finally {
            currentFrameIndex = originalFrameIndex.coerceIn(0, frames.lastIndex)
            restoreFrame(currentFrameIndex)
            isPreviewing = false
        }
    }

    private fun snapshot(): GlyphCanvasSnapshot {
        commitCurrentFrame()

        return GlyphCanvasSnapshot(
            frames = frames.map { it.toList() },
            currentFrameIndex = currentFrameIndex,
            brushBrightness = brushBrightness,
            animationFrameTime = animationFrameTime
        )
    }

    private fun restoreSnapshot(
        snapshot: GlyphCanvasSnapshot
    ) {
        frames.clear()
        frames.addAll(
            snapshot.frames.ifEmpty {
                listOf(editor.getEmptyFrame())
            }
        )

        currentFrameIndex = snapshot.currentFrameIndex.coerceIn(0, frames.lastIndex.coerceAtLeast(0))
        brushBrightness = snapshot.brushBrightness.coerceIn(1, 100)
        animationFrameTime = snapshot.animationFrameTime.coerceAtLeast(1)

        restoreFrame(currentFrameIndex)
    }

    private fun pushUndoSnapshot(
        before: GlyphCanvasSnapshot
    ) {
        if (before == snapshot()) return

        undoHistory.add(before)
        trimHistory(undoHistory)

        redoHistory.clear()
    }

    private fun trimHistory(
        history: MutableList<GlyphCanvasSnapshot>
    ) {
        while (history.size > GlyphCanvasConstants.MAX_HISTORY_SNAPSHOTS) {
            history.removeAt(0)
        }
    }

    private fun commitCurrentFrame() {
        if (frames.isEmpty()) {
            frames.add(editor.getEmptyFrame())
        }

        val safeIndex = currentFrameIndex.coerceIn(0, frames.lastIndex)
        currentFrameIndex = safeIndex
        frames[safeIndex] = cells.toList()
    }

    private fun restoreFrame(
        index: Int
    ) {
        val frame = frames.getOrNull(index) ?: editor.getEmptyFrame()

        cells.clear()
        cells.addAll(
            frame.mapIndexed { cellIndex, value ->
                if (editor.mask[cellIndex]) {
                    value.coerceIn(0, 100)
                } else {
                    0
                }
            }
        )
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

    val persistScope = rememberCoroutineScope()

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
            persistScope = persistScope,
            cells = cells
        )
    }

    LaunchedEffect(Unit) {
        state.loadDraft()
    }

    return state
}
