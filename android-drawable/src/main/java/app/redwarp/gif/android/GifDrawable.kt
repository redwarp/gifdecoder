/* Copyright 2020 Benoit Vermont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.redwarp.gif.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import app.redwarp.gif.decoder.Gif
import app.redwarp.gif.decoder.Parser
import app.redwarp.gif.decoder.descriptors.GifDescriptor
import app.redwarp.gif.decoder.descriptors.params.LoopCount
import app.redwarp.gif.decoder.descriptors.params.PixelPacking
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * A drawable that can be used to display a static or animated GIF.
 *
 * Usage:
 * ```kotlin
 * val inputStream:InputStream = (...)
 *
 * val gifDrawable = GifDrawable.from(inputStream)
 * ```
 *
 * By default, the drawable will respect the loop count of the decoded GIF.
 * You can force your loop count by doing
 * ```kotlin
 * gifDrawable.loopCount = LoopCount.INFINITE
 * ```
 */
class GifDrawable(gifDescriptor: GifDescriptor) : Drawable(), Animatable2Compat {
    private val state = GifDrawableState(gifDescriptor)

    private val bitmapCache = BitmapPool.obtain()

    private val animationCallbacks = mutableListOf<Animatable2Compat.AnimationCallback>()
    private val lock = Object()
    private val didRender = Channel<Unit>(capacity = 1)

    @Volatile
    private var isRunning: Boolean = false
    private var loopJob: Job? = null
    private val gifWidth = state.gif.dimension.width
    private val gifHeight = state.gif.dimension.height

    private val pixels = IntArray(state.gif.dimension.size)
    private var bitmap: Bitmap? = getCurrentFrame()
        set(value) {
            synchronized(lock) {
                field = value
            }
        }

    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = bitmap?.config == Bitmap.Config.RGB_565
    }
    private val matrix = Matrix()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineName("GifDrawable"))

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    /**
     * @since 0.7.0
     */
    var loopCount: LoopCount
        get() = state.loopCount ?: state.gif.loopCount
        set(value) {
            state.loopCount = value
        }

    fun backgroundColor(): Int {
        return state.gif.backgroundColor
    }

    override fun getIntrinsicWidth(): Int {
        return if (state.gif.aspectRatio >= 1.0) {
            (gifWidth.toDouble() * state.gif.aspectRatio).toInt()
        } else {
            gifWidth
        }
    }

    override fun getIntrinsicHeight(): Int {
        return if (state.gif.aspectRatio >= 1.0) {
            gifHeight
        } else {
            (gifHeight.toDouble() / state.gif.aspectRatio).toInt()
        }
    }

    override fun draw(canvas: Canvas) {
        synchronized(lock) {
            val checkpoint = canvas.save()
            canvas.concat(matrix)
            bitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
            }
            canvas.restoreToCount(checkpoint)
            didRender.trySend(Unit)
        }

        if (isRunning && loopJob?.isActive != true) {
            loopJob = coroutineScope.launch {
                animationLoop()
            }.apply {
                invokeOnCompletion {
                    loopJob = null
                }
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

        isRunning = true
        postAnimationStart()

        invalidateSelf()
    }

    override fun stop() {
        if (!isRunning) return // Already stopped.
        loopJob?.cancel()
        isRunning = false
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

    override fun getConstantState(): ConstantState {
        return state
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
        if (animationCallbacks.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                animationCallbacks.forEach { callback ->
                    callback.onAnimationStart(this@GifDrawable)
                }
            }
        }
    }

    private fun postAnimationEnd() {
        if (animationCallbacks.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                animationCallbacks.forEach { callback ->
                    callback.onAnimationEnd(this@GifDrawable)
                }
            }
        }
    }

    private fun shouldAnimate(): Boolean {
        if (!state.gif.isAnimated) return false

        return when (val repeatCount = loopCount) {
            is LoopCount.NoLoop -> false
            is LoopCount.Fixed -> state.loopIteration < repeatCount.count
            is LoopCount.Infinite -> true
        }
    }

    private suspend fun animationLoop() = withContext(Dispatchers.IO) loop@{
        if (!shouldAnimate()) {
            postAnimationEnd()
            isRunning = false
            return@loop
        }

        var startTime = SystemClock.elapsedRealtime()
        while (true) {
            if (!isRunning) return@loop

            val frameDelay = state.gif.currentDelay.let {
                // If the frame delay is 0, let's at last have 2 frame before we display it.
                if (it == 0L) 32L else it
            }
            state.gif.advance()
            if (state.gif.currentIndex == 0) {
                // We looped back to the first frame
                state.loopIteration++
            }
            // Checking if we are finished looping already
            if (!shouldAnimate()) {
                postAnimationEnd()
                isRunning = false
                return@loop
            }

            val nextFrame = getCurrentFrame()
            if (nextFrame == null) {
                postAnimationEnd()
                isRunning = false
                return@loop
            }
            nextFrame.prepareToDraw()

            val elapsedTime: Long = SystemClock.elapsedRealtime() - startTime
            val delay: Long = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)

            delay(delay)

            startTime = SystemClock.elapsedRealtime()

            didRender.receive()
            synchronized(lock) {
                val oldBitmap = bitmap
                bitmap = nextFrame
                bitmapPaint.isDither = nextFrame.config == Bitmap.Config.RGB_565

                bitmapCache.release(oldBitmap)
            }
            withContext(Dispatchers.Main) {
                invalidateSelf()
            }
        }
    }

    private fun getCurrentFrame(): Bitmap? {
        if (state.gif.getCurrentFrame(pixels).isFailure) return null

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

        /**
         * Creates a GifDrawable from an [InputStream]
         */
        fun from(inputStream: InputStream): Result<GifDrawable> =
            Parser.parse(inputStream, PixelPacking.ARGB).map(::GifDrawable)

        fun from(file: File): Result<GifDrawable> =
            Parser.parse(file, PixelPacking.ARGB).map(::GifDrawable)
    }

    private class GifDrawableState(private val gifDescriptor: GifDescriptor) : ConstantState() {
        val gif = Gif(gifDescriptor)
        var loopCount: LoopCount? = null
        var loopIteration = 0

        override fun newDrawable(): Drawable {
            return GifDrawable(gifDescriptor).also { copiedDrawable ->
                copiedDrawable.state.loopCount = loopCount
                copiedDrawable.state.loopIteration = loopIteration
            }
        }

        // No need to recreate the drawable for any configurations.
        override fun getChangingConfigurations(): Int = 0
    }
}
