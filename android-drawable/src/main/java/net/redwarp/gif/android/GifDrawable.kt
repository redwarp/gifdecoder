package net.redwarp.gif.android

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import androidx.core.graphics.withMatrix
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.redwarp.gif.decoder.Gif
import net.redwarp.gif.decoder.LoopCount
import net.redwarp.gif.decoder.Parser
import net.redwarp.gif.decoder.PixelPacking
import net.redwarp.gif.decoder.descriptors.GifDescriptor
import java.io.InputStream

private const val DRAW_MESSAGE = 1024

class GifDrawable(gifDescriptor: GifDescriptor) : Drawable(), Animatable2Compat {

    private val gif = Gif(gifDescriptor)

    private val bitmapCache = BitmapPool.obtain()

    private val animationCallbacks = mutableListOf<Animatable2Compat.AnimationCallback>()
    private val lock = Object()

    @Volatile
    private var isRunning: Boolean = false
    private var loopJob: Job? = null
    private val gifWidth = gif.dimension.width
    private val gifHeight = gif.dimension.height

    private val pixels = IntArray(gif.dimension.size)
    private var bitmap: Bitmap = getCurrentFrame()

    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = bitmap.config == Bitmap.Config.RGB_565
        shader = BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP)
    }
    private val matrix = Matrix()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineName("GifDrawable"))

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    private var loopIteration = 0
    private var _loopCount: LoopCount? = null
    private val handler: Handler by lazy {
        DrawHandler(Looper.getMainLooper())
    }

    fun setRepeatCount(repeatCount: Int) {
        _loopCount = when {
            repeatCount == REPEAT_INFINITE -> {
                LoopCount.Infinite
            }
            repeatCount > 0 -> {
                LoopCount.Fixed(repeatCount)
            }
            else -> LoopCount.NoLoop
        }
    }

    fun getRepeatCount(): Int {
        return when (val loopCount = _loopCount ?: gif.loopCount) {
            LoopCount.Infinite -> REPEAT_INFINITE
            LoopCount.NoLoop -> 0
            is LoopCount.Fixed -> loopCount.count
        }
    }

    fun backgroundColor(): Int {
        return gif.backgroundColor
    }

    override fun getIntrinsicWidth(): Int {
        return if (gif.aspectRatio >= 1.0) {
            (gifWidth.toDouble() * gif.aspectRatio).toInt()
        } else {
            gifWidth
        }
    }

    override fun getIntrinsicHeight(): Int {
        return if (gif.aspectRatio >= 1.0) {
            gifHeight
        } else {
            (gifHeight.toDouble() / gif.aspectRatio).toInt()
        }
    }

    override fun draw(canvas: Canvas) {
        synchronized(lock) {
            canvas.withMatrix(matrix) {
                drawRect(
                    0f,
                    0f,
                    gifWidth.toFloat(),
                    gifHeight.toFloat(),
                    bitmapPaint
                )
            }
        }

        if (isRunning && !handler.hasMessages(DRAW_MESSAGE) && !(loopJob?.isActive == true)) {
            loopJob = coroutineScope.launch {
                animationJob()
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        if (isRunning) return // Already running.

        // No need to animate gifs with single frame, or already finished gifs.
        if (!shouldAnimate()) return

        // loopJob?.cancel()

        isRunning = true
        postAnimationStart()

        invalidateSelf()
    }

    override fun stop() {
        if (!isRunning) return // Already stopped.

        isRunning = false
        loopJob?.cancel()
        handler.removeMessages(DRAW_MESSAGE)
        postAnimationEnd()
    }

    override fun isRunning(): Boolean = isRunning

    override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
        animationCallbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
        return animationCallbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() {
        animationCallbacks.clear()
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        matrix.setRectToRect(
            RectF(0f, 0f, gifWidth.toFloat(), gifHeight.toFloat()),
            RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()),
            Matrix.ScaleToFit.FILL
        )
    }

    private fun postAnimationStart() {
        if (animationCallbacks.isNotEmpty())
            handler.post {
                animationCallbacks.forEach { callback ->
                    callback.onAnimationStart(this)
                }
            }
    }

    private fun postAnimationEnd() {
        if (animationCallbacks.isNotEmpty())
            handler.post {
                animationCallbacks.forEach { callback ->
                    callback.onAnimationEnd(this)
                }
            }
    }

    private fun shouldAnimate(): Boolean {
        if (!gif.isAnimated) return false

        val repeatCount = getRepeatCount()
        return repeatCount != 0 || loopIteration < repeatCount
    }

    private fun animationJob() {
        val startTime = SystemClock.elapsedRealtime()
        if (handler.hasMessages(DRAW_MESSAGE)) return

        if (!shouldAnimate()) {
            postAnimationEnd()
            isRunning = false
            return
        }

        val frameDelay = gif.currentDelay.let {
            // If the frame delay is 0, let's at last have 2 frame before we display it.
            if (it == 0L) 32L else it
        }
        gif.advance()
        if (gif.currentIndex == 0) {
            // We looped back to the first frame
            loopIteration++
        }
        // Checking if we are finished looping already
        if (!shouldAnimate()) {
            postAnimationEnd()
            isRunning = false
            return
        }

        val nextFrame = getCurrentFrame()
        nextFrame.prepareToDraw()

        val elapsedTime: Long = SystemClock.elapsedRealtime() - startTime
        val delay: Long = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)

        val message = handler.obtainMessage(DRAW_MESSAGE, nextFrame)
        handler.sendMessageDelayed(message, delay)
    }

    private fun getCurrentFrame(): Bitmap {
        gif.getCurrentFrame(pixels)

        val transparent = pixels.any { it == 0 }

        val nextFrame: Bitmap = bitmapCache.obtain(
            width = gifWidth,
            height = gifHeight,
            config = if (transparent) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )

        nextFrame.setPixels(pixels, 0, gifWidth, 0, 0, gifWidth, gifHeight)
        return nextFrame
    }

    companion object {
        const val REPEAT_INFINITE = -1

        fun from(inputStream: InputStream): GifDrawable =
            GifDrawable(Parser.parse(inputStream, PixelPacking.ARGB))
    }

    private inner class DrawHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            (msg.obj as? Bitmap)?.let { nextFrame ->
                synchronized(lock) {
                    val oldBitmap = bitmap
                    bitmap = nextFrame
                    bitmapPaint.shader =
                        BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP)
                    bitmapPaint.isDither = bitmap.config == Bitmap.Config.RGB_565

                    bitmapCache.release(oldBitmap)
                }

                invalidateSelf()
            }
        }
    }
}
