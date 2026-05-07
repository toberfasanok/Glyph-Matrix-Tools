package com.tober.glyphmatrixtools.glyph

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

import com.nothing.ketchum.Glyph as NothingGlyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

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

    private data class GlyphRequest(
        val glyph: Glyph,
        val timeout: Long? = 0L
    )
    private var pendingRequest: GlyphRequest? = null
    private var currentGlyph: Glyph? = null
    private var currentBitmap: Bitmap? = null

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

        Log.d(tag, "Start")

        manager = GlyphMatrixManager.getInstance(context.applicationContext)
        manager?.init(managerCallback)
    }

    fun stop() {
        Log.d(tag, "Stop")

        stopRunnable()
        closeMatrix()

        try {
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(tag, "Failed to unInit: $e")
        }

        initialized = false
        manager = null
        pendingRequest = null
        clearState()
    }

    fun show(
        glyph: Glyph,
        timeout: Long?
    ) {
        mainHandler.post {
            showInternal(
                glyph,
                timeout
            )
        }
    }

    fun showPersistent(
        glyph: Glyph
    ) {
        mainHandler.post {
            showInternal(
                glyph = glyph,
                timeout = null
            )
        }
    }

    fun clear() {
        mainHandler.post {
            pendingRequest = null

            val glyph = currentGlyph
            val bitmap = currentBitmap

            stopRunnable()

            if (glyph?.imageAnimate == true && bitmap != null && initialized) {

                hideAnimated(
                    glyphBitmap = bitmap,
                    speed = getCircleAnimationSpeed(),
                    startRadius = currentRadius.coerceIn(minRadius, maxRadius),
                    ::clearMatrixAndState
                )
            } else {
                clearMatrixAndState()
            }
        }
    }

    private fun showInternal(
        glyph: Glyph,
        timeout: Long?
    ) {
        if (glyph.image.isNullOrBlank()) return

        if (!initialized) {
            pendingRequest = GlyphRequest(
                glyph,
                timeout
            )
            start()
            return
        }

        stopRunnable()
        closeMatrix()

        val bitmap = BitmapFactory.decodeFile(glyph.image) ?: return

        currentGlyph = glyph
        currentBitmap = bitmap
        currentRadius = minRadius

        if (glyph.imageAnimate) {
            showAnimated(
                glyphBitmap = bitmap,
                timeout = timeout,
                speed = getCircleAnimationSpeed(),
                ::clearMatrixAndState
            )
        } else {
            showSimple(
                glyphBitmap = bitmap,
                timeout = timeout,
                ::clearMatrixAndState
            )
        }
    }

    private fun showSimple(
        glyphBitmap: Bitmap,
        timeout: Long?,
        operation: () -> Unit
    ) {
        currentRadius = maxRadius

        renderBitmap(glyphBitmap)

        if (timeout == null) return

        val runnable = Runnable {
            operation()
            clearRunnable = null
        }

        clearRunnable = runnable
        mainHandler.postDelayed(runnable, timeout)
    }

    private fun showAnimated(
        glyphBitmap: Bitmap,
        timeout: Long?,
        speed: Long,
        operation: () -> Unit
    ) {
        val scaledBitmap = glyphBitmap.scale(matrixSize, matrixSize)

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

                    val maskedBitmap = getMaskedBitmap(
                        radius = radius,
                        bitmap = scaledBitmap
                    )

                    renderBitmap(maskedBitmap)

                    frameIndex++

                    mainHandler.postDelayed(this, plan.frameDelay)
                } else {
                    currentRadius = maxRadius

                    animationRunnable = null

                    if (timeout == null) return

                    val clear = Runnable {
                        hideAnimated(
                            glyphBitmap = glyphBitmap,
                            speed = speed,
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

    private fun hideAnimated(
        glyphBitmap: Bitmap,
        speed: Long,
        startRadius: Float = maxRadius,
        operation: () -> Unit
    ) {
        clearRunnable = null

        val scaledBitmap = glyphBitmap.scale(matrixSize, matrixSize)

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

                    val maskedBitmap = getMaskedBitmap(
                        radius = radius,
                        bitmap = scaledBitmap
                    )

                    renderBitmap(maskedBitmap)

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

                val brightness = (255 * coverage)
                    .roundToInt()
                    .coerceIn(0, 255)

                maskedBitmap[x, y] = Color.argb(
                    255,
                    brightness,
                    brightness,
                    brightness
                )
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

    private fun stopRunnable() {
        clearRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        animationRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        clearRunnable = null
        animationRunnable = null
    }

    private fun closeMatrix() {
        try {
            manager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.e(tag, "Failed to close matrix: $e")
        }
    }

    private fun clearState() {
        currentGlyph = null
        currentBitmap = null
        currentRadius = minRadius
    }

    private fun clearMatrixAndState() {
        closeMatrix()
        clearState()
    }
}
