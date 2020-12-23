package net.redwarp.gif.decoder.android

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import kotlinx.coroutines.*
import net.redwarp.gif.decoder.Gif
import net.redwarp.gif.decoder.Parser
import okio.Source
import okio.source
import java.io.File
import java.io.InputStream
import kotlin.coroutines.coroutineContext

class GifDrawable(source: Source) : Drawable(), Animatable2Compat {
    constructor(inputStream: InputStream) : this(inputStream.source())
    constructor(file: File) : this(file.source())

    private val gifDescriptor = Parser.parse(source)
    private val gif = Gif(gifDescriptor)

    private val bitmapCache = BitmapCache()

    private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    private var isRunning: Boolean = false
    private var loopJob: Job? = null
    private val width = gif.dimension.width
    private val height = gif.dimension.height

    private val pixels = IntArray(gif.dimension.size)
    private var bitmap: Bitmap = getCurrentFrame()

    val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height


    override fun draw(canvas: Canvas) {
        bitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, bounds, bitmapPaint)
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

        val previousJob = loopJob
        loopJob = CoroutineScope(Dispatchers.Default).launch {
            previousJob?.cancelAndJoin()
            animationLoop()
        }

        callbacks.forEach { callback ->
            callback.onAnimationStart(this)
        }
    }

    override fun stop() {
        if (!isRunning) return // Already stopped.

        synchronized(this) {
            loopJob?.cancel()
        }

        callbacks.forEach { callback ->
            callback.onAnimationEnd(this)
        }
    }

    override fun isRunning(): Boolean = isRunning

    override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
        callbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
        return callbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() {
        callbacks.clear()
    }


    private suspend fun animationLoop() {
        if (!gif.isAnimated) {
            bitmap = getCurrentFrame()

            invalidateSelf()

            return
        }

        while (true) {
            coroutineContext.ensureActive()
            val frameDelay = gif.currentDelay
            gif.advance()
            val startTime = SystemClock.elapsedRealtime()

            val nextFrame = getCurrentFrame()

            val elapsedTime: Long = SystemClock.elapsedRealtime() - startTime
            val delay: Long = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)


            coroutineContext.ensureActive()
            delay(delay)

            synchronized(this) {
                coroutineContext.ensureActive()
                val oldBitmap = bitmap
                bitmap = nextFrame

                bitmapCache.release(oldBitmap)

                invalidateSelf()
            }
        }
    }

    private fun getCurrentFrame(): Bitmap {
        gif.getCurrentFrame(pixels)
        val nextFrame: Bitmap = bitmapCache.obtain(width, height)
        nextFrame.setPixels(pixels, 0, width, 0, 0, width, height)
        return nextFrame
    }
}