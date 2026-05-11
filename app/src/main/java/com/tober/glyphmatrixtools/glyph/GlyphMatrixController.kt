package com.tober.glyphmatrixtools.glyph

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

import com.nothing.ketchum.Glyph as NothingGlyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

import com.tober.glyphmatrixtools.canvas.GlyphCanvasAnimationFile
import com.tober.glyphmatrixtools.canvas.GlyphCanvasEditor
import com.tober.glyphmatrixtools.util.Constants
import com.tober.glyphmatrixtools.util.PreferencesService

class GlyphMatrixController(
    private val context: Context
) {
    private val tag = "Glyph Matrix Controller"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferencesService = PreferencesService(context)

    private var clearRunnable: Runnable? = null
    private var animationRunnable: Runnable? = null

    private var initialized = false
    private var manager: GlyphMatrixManager? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private val wakeLockTimeout = 60 * 60 * 1000L

    private data class GlyphRequest(
        val glyph: Glyph,
        val timeout: Long? = 0L
    )
    private var pendingRequest: GlyphRequest? = null
    private var currentGlyph: Glyph? = null
    private data class GlyphPresentation(
        val frames: List<Bitmap>,
        val frameTime: Long
    ) {
        val isAnimation: Boolean
            get() = frames.size > 1
    }

    private var currentPresentation: GlyphPresentation? = null
    private var currentFrameIndex = 0
    private var playbackRunnable: Runnable? = null

    private val canvasEditor = GlyphCanvasEditor()

    private val matrixSize = 25
    private val matrixCenterX = (matrixSize - 1) / 2.0
    private val matrixCenterY = (matrixSize - 1) / 2.0

    /**
     * This value controls the softness of the circle edge. A larger value creates
     * a wider fade between invisible and fully visible pixels, making the circle
     * look smoother on the low-resolution matrix.
     */
    private val maskEdgeWidth = 2f

    /**
     * This value controls how much the circle radius changes between full-quality
     * animation frames. Smaller values create more candidate frames and smoother
     * movement, while larger values create fewer frames and faster movement.
     */
    private val radiusStep = 0.25f

    /**
     * The radius starts at a negative value because if it were to start at 0 instead,
     * the center pixel of the matrix would instantly be 100% in brightness. A negative
     * value ensures that the start of the circle animation is gradual from partially
     * visible values to fully visible ones.
     */
    private val minRadius = -maskEdgeWidth
    private val maxRadius = sqrt(matrixCenterX * matrixCenterX + matrixCenterY * matrixCenterY).toFloat()
    private var currentRadius = minRadius

    /**
     * This value represents the minimal frame rate at which the animation is able to
     * keep up. With rising animationFramesPerRadius, the number of frames increases,
     * and the time between each of them decreases. After this value goes below this
     * threshold, skipping of frames is started so that the animation can keep up.
     */
    private val minimumUsefulFrameTime = 8L

    /**
     * This describes the final playback plan for one animation pass. The frame count
     * may be lower than the full-quality candidate frame count when the requested
     * animation speed is too fast to display every frame reliably.
     */
    private data class AnimationPlan(
        val frameCount: Int,
        val frameDelay: Long
    )

    private companion object {
        private const val FAST_CIRCLE_ANIMATION_SPEED = 10L
    }

    private val managerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(
            componentName: ComponentName?
        ) {
            Log.d(tag, "Connected: $componentName")

            try {
                manager?.register(NothingGlyph.DEVICE_23112)
                initialized = true

                Log.d(tag, "Initialized")

                pendingRequest?.let { request ->
                    pendingRequest = null
                    show(
                        glyph = request.glyph,
                        timeout = request.timeout
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed initialization: $e")
            }
        }

        override fun onServiceDisconnected(
            componentName: ComponentName?
        ) {
            Log.d(tag, "Disconnected: $componentName")
            initialized = false
        }
    }

    fun start() {
        if (manager != null) return

        Log.d(tag, "Started")

        manager = GlyphMatrixManager.getInstance(context.applicationContext)
        manager?.init(managerCallback)
    }

    fun stop() {
        Log.d(tag, "Stopped")

        cancelScheduledDisplayWork()
        finishDisplay()

        try {
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(tag, "Failed to unInit: $e")
        }

        initialized = false
        manager = null
        pendingRequest = null
        resetActiveGlyphState()
    }

    fun clear(
        onCleared: () -> Unit = {}
    ) {
        Log.d(tag, "clear")

        mainHandler.post {
            val speed = preferencesService
                .getLong(Constants.PREFERENCES_CIRCLE_ANIMATION_SPEED, 50L)
                .coerceAtLeast(1L)

            clearInternal(
                speed = speed,
                onCleared = onCleared
            )
        }
    }

    fun clearFast(
        onCleared: () -> Unit = {}
    ) {
        Log.d(tag, "clearFast")

        mainHandler.post {
            clearInternal(
                speed = FAST_CIRCLE_ANIMATION_SPEED,
                onCleared = onCleared
            )
        }
    }

    private fun clearInternal(
        speed: Long,
        onCleared: () -> Unit
    ) {
        pendingRequest = null

        val glyph = currentGlyph
        val presentation = currentPresentation

        cancelTimeoutAndCircleWork()

        if (glyph?.circleAnimate == true && presentation != null && initialized) {
            hideCircleReveal(
                speed = speed,
                startRadius = currentRadius,
                operation = {
                    finishDisplay()
                    onCleared()
                }
            )
        } else {
            finishDisplay()
            onCleared()
        }
    }

    fun show(
        glyph: Glyph,
        timeout: Long?,
        onFinished: () -> Unit = {}
    ) {
        Log.d(tag, "show")

        mainHandler.post {
            showInternal(
                glyph,
                timeout,
                onFinished = onFinished
            )
        }
    }

    fun showPersistent(
        glyph: Glyph
    ) {
        Log.d(tag, "showPersistent")

        mainHandler.post {
            showInternal(
                glyph = glyph,
                timeout = null,
                onFinished = {}
            )
        }
    }

    private fun showInternal(
        glyph: Glyph,
        timeout: Long?,
        onFinished: () -> Unit
    ) {
        val presentation = loadPresentation(glyph) ?: return

        if (!initialized) {
            pendingRequest = GlyphRequest(
                glyph,
                timeout
            )
            start()
            return
        }

        prepareForNewDisplay()
        acquireWakeLock()

        currentGlyph = glyph
        currentPresentation = presentation
        currentFrameIndex = 0
        currentRadius = if (glyph.circleAnimate) {
            minRadius
        } else {
            maxRadius
        }

        renderCurrentPresentationFrame()

        if (presentation.isAnimation) {
            startPresentationPlayback()
        }

        if (glyph.circleAnimate) {
            showCircleReveal(
                timeout = timeout,
                speed = getCircleAnimationSpeed(),
                operation = {
                    finishDisplay()
                    onFinished()
                }
            )
        } else {
            showWithTimeout(
                timeout = timeout,
                operation = {
                    finishDisplay()
                    onFinished()
                }
            )
        }
    }

    private fun showWithTimeout(
        timeout: Long?,
        operation: () -> Unit
    ) {
        if (timeout == null) return

        val runnable = Runnable {
            operation()
            clearRunnable = null
        }

        clearRunnable = runnable
        mainHandler.postDelayed(runnable, timeout)
    }

    private fun showCircleReveal(
        timeout: Long?,
        speed: Long,
        operation: () -> Unit
    ) {
        Log.d(tag, "showCircleReveal")

        val plan = getAnimationPlan(
            speed = speed,
            startRadius = minRadius,
            endRadius = maxRadius
        )

        var frameIndex = 0

        val runnable = object : Runnable {
            override fun run() {
                if (frameIndex < plan.frameCount) {
                    val radius = getAnimationRadius(
                        startRadius = minRadius,
                        endRadius = maxRadius,
                        frameIndex = frameIndex,
                        frameCount = plan.frameCount
                    )

                    currentRadius = radius
                    renderCurrentPresentationFrame()

                    frameIndex++

                    mainHandler.postDelayed(this, plan.frameDelay)
                } else {
                    currentRadius = maxRadius
                    renderCurrentPresentationFrame()

                    animationRunnable = null

                    if (timeout == null) return

                    val clear = Runnable {
                        hideCircleReveal(
                            speed = speed,
                            startRadius = maxRadius,
                            operation = operation
                        )
                    }

                    clearRunnable = clear
                    mainHandler.postDelayed(clear, timeout)
                }
            }
        }

        animationRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun hideCircleReveal(
        speed: Long,
        startRadius: Float = maxRadius,
        operation: () -> Unit
    ) {
        Log.d(tag, "hideCircleReveal")

        clearRunnable = null

        val safeStartRadius = startRadius.coerceIn(minRadius, maxRadius)

        val plan = getAnimationPlan(
            speed = speed,
            startRadius = safeStartRadius,
            endRadius = minRadius
        )

        var frameIndex = 0

        val runnable = object : Runnable {
            override fun run() {
                if (frameIndex < plan.frameCount) {
                    val radius = getAnimationRadius(
                        startRadius = safeStartRadius,
                        endRadius = minRadius,
                        frameIndex = frameIndex,
                        frameCount = plan.frameCount
                    )

                    currentRadius = radius
                    renderCurrentPresentationFrame()

                    frameIndex++

                    mainHandler.postDelayed(this, plan.frameDelay)
                } else {
                    animationRunnable = null
                    operation()
                }
            }
        }

        animationRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun renderBitmap(
        glyphBitmap: Bitmap
    ) {
        try {
            val objBuilder = GlyphMatrixObject.Builder()
            val image = objBuilder
                .setImageSource(glyphBitmap)
                .setScale(100)
                .setOrientation(0)
                .setPosition(0, 0)
                .setReverse(false)
                .build()

            val frameBuilder = GlyphMatrixFrame.Builder()
            val frame = frameBuilder.addTop(image).build(context)
            val rendered = frame.render()

            manager?.setAppMatrixFrame(rendered)
        } catch (e: Exception) {
            Log.e(tag, "Failed to render glyph: $e")
        }
    }

    private fun getMaskedBitmap(
        radius: Float,
        bitmap: Bitmap
    ): Bitmap {
        val maskedBitmap = createBitmap(matrixSize, matrixSize)

        for (y in 0 until matrixSize) {
            val dy = y - matrixCenterY
            val dySq = dy * dy

            for (x in 0 until matrixSize) {
                val dx = x - matrixCenterX
                val distance = sqrt(dx * dx + dySq).toFloat()

                val coverage = ((radius + maskEdgeWidth - distance) / maskEdgeWidth)
                    .coerceIn(0f, 1f)

                if (coverage <= 0f) {
                    maskedBitmap[x, y] = 0
                    continue
                }

                val sourcePixel = bitmap[x, y]
                val sourceAlpha = Color.alpha(sourcePixel)

                if (sourceAlpha == 0) {
                    maskedBitmap[x, y] = 0
                    continue
                }

                val sourceRed = Color.red(sourcePixel)
                val sourceGreen = Color.green(sourcePixel)
                val sourceBlue = Color.blue(sourcePixel)

                val outputRed = (sourceRed * coverage)
                    .roundToInt()
                    .coerceIn(0, 255)

                val outputGreen = (sourceGreen * coverage)
                    .roundToInt()
                    .coerceIn(0, 255)

                val outputBlue = (sourceBlue * coverage)
                    .roundToInt()
                    .coerceIn(0, 255)

                if (outputRed <= 0 && outputGreen <= 0 && outputBlue <= 0) {
                    maskedBitmap[x, y] = 0
                } else {
                    maskedBitmap[x, y] = Color.argb(
                        255,
                        outputRed,
                        outputGreen,
                        outputBlue
                    )
                }
            }
        }

        return maskedBitmap
    }

    private fun getAnimationPlan(
        speed: Long,
        startRadius: Float,
        endRadius: Float
    ): AnimationPlan {
        val radiusRange = abs(endRadius - startRadius).coerceAtLeast(0.001f)
        val fullQualityFrameCount = ceil(radiusRange / radiusStep)
            .toInt()
            .coerceAtLeast(1) + 1

        val desiredDuration = (radiusRange * speed)
            .roundToLong()
            .coerceAtLeast(1L)

        val maxUsefulFrameCount = (desiredDuration / minimumUsefulFrameTime)
            .toInt()
            .coerceAtLeast(2)

        val frameCount = minOf(
            fullQualityFrameCount,
            maxUsefulFrameCount
        )

        val frameDelay = desiredDuration / frameCount

        return AnimationPlan(
            frameCount = frameCount,
            frameDelay = frameDelay
        )
    }

    private fun getAnimationRadius(
        startRadius: Float,
        endRadius: Float,
        frameIndex: Int,
        frameCount: Int
    ): Float {
        if (frameCount <= 1) return endRadius

        val progress = frameIndex.toFloat() / (frameCount - 1).toFloat()

        return startRadius + ((endRadius - startRadius) * progress)
    }

    private fun getCircleAnimationSpeed(): Long {
        return preferencesService
            .getLong(Constants.PREFERENCES_CIRCLE_ANIMATION_SPEED, 25L)
            .coerceAtLeast(1L)
    }

    private fun prepareForNewDisplay() {
        cancelScheduledDisplayWork()
        closeGlyphMatrixSafely()
        resetActiveGlyphState()
    }

    private fun finishDisplay() {
        stopPresentationPlayback()
        closeGlyphMatrixSafely()
        resetActiveGlyphState()
        releaseWakeLock()
    }

    private fun cancelScheduledDisplayWork() {
        cancelTimeoutAndCircleWork()
        stopPresentationPlayback()
    }

    private fun cancelTimeoutAndCircleWork() {
        clearRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        animationRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        clearRunnable = null
        animationRunnable = null
    }

    private fun closeGlyphMatrixSafely() {
        try {
            manager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.e(tag, "Failed to close Glyph Matrix: $e")
        }
    }

    private fun resetActiveGlyphState() {
        currentGlyph = null
        currentPresentation = null
        currentFrameIndex = 0
        currentRadius = minRadius
    }

    private fun loadPresentation(
        glyph: Glyph
    ): GlyphPresentation? {
        val hasImage = !glyph.image.isNullOrBlank()
        val hasAnimation = !glyph.animation.isNullOrBlank()

        if (hasImage == hasAnimation) {
            Log.e(tag, "Glyph must have exactly one asset")
            return null
        }

        if (hasImage) {
            val bitmap = BitmapFactory
                .decodeFile(glyph.image)
                ?.scale(matrixSize, matrixSize)
                ?: return null

            return GlyphPresentation(
                frames = listOf(bitmap),
                frameTime = 0L
            )
        }

        val raw = runCatching {
            File(glyph.animation!!).readText()
        }.getOrNull() ?: return null

        val animationFile = GlyphCanvasAnimationFile.fromJson(raw) ?: return null

        val frames = animationFile.frames
            .map { frame ->
                canvasEditor
                    .toBitmap(frame)
                    .scale(matrixSize, matrixSize)
            }
            .filter {
                it.width > 0 && it.height > 0
            }

        if (frames.isEmpty()) return null

        return GlyphPresentation(
            frames = frames,
            frameTime = animationFile.frameTime.toLong().coerceAtLeast(1L)
        )
    }

    private fun startPresentationPlayback() {
        stopPresentationPlayback()

        val presentation = currentPresentation ?: return

        if (!presentation.isAnimation) return

        val runnable = object : Runnable {
            override fun run() {
                val current = currentPresentation ?: return
                if (!current.isAnimation) return

                currentFrameIndex = (currentFrameIndex + 1) % current.frames.size

                renderCurrentPresentationFrame()

                mainHandler.postDelayed(
                    this,
                    current.frameTime.coerceAtLeast(1L)
                )
            }
        }

        playbackRunnable = runnable
        mainHandler.postDelayed(
            runnable,
            presentation.frameTime.coerceAtLeast(1L)
        )
    }

    private fun stopPresentationPlayback() {
        playbackRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        playbackRunnable = null
    }

    private fun renderCurrentPresentationFrame() {
        val presentation = currentPresentation ?: return
        val frame = presentation.frames.getOrNull(
            currentFrameIndex.coerceIn(0, presentation.frames.lastIndex)
        ) ?: return

        val bitmap = if (currentRadius >= maxRadius) {
            frame
        } else {
            getMaskedBitmap(
                radius = currentRadius,
                bitmap = frame
            )
        }

        renderBitmap(bitmap)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock(
        timeout: Long? = null
    ) {
        try {
            val powerManager = context.getSystemService(PowerManager::class.java)

            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "GlyphMatrixTools:GlyphMatrixController"
                ).apply {
                    setReferenceCounted(false)
                }
            }

            val lock = wakeLock ?: return

            if (!lock.isHeld) {
                if (timeout != null) {
                    lock.acquire(timeout)
                } else {
                    lock.acquire(wakeLockTimeout)
                }

                Log.d(tag, "WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire WakeLock: $e")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(tag, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to release WakeLock: $e")
        } finally {
            wakeLock = null
        }
    }
}
