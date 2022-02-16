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
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

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

    private val bitmapCache = BitmapCache()
    private val animationCallbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    @Volatile
    private var isRunning: Boolean = false
    private var prepareFrameFuture: Future<Bitmap?>? = null
    private val gifWidth get() = state.gif.dimension.width
    private val gifHeight get() = state.gif.dimension.height

    private val pixels = IntArray(state.gif.dimension.size)
    private var bitmap: Bitmap? = getCurrentFrame()

    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = bitmap?.config == Bitmap.Config.RGB_565
    }
    private val matrix = Matrix()

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    /**
     * @since 0.7.0
     */
    var loopCount: LoopCount
        get() = synchronized(state) { state.loopCount ?: state.gif.loopCount }
        set(value) = synchronized(state) {
            state.loopCount = value
            state.loopIteration = 0
        }

    /**
     * @since 0.9.0
     */
    val backgroundColor: Int
        get() = state.gif.backgroundColor

    @Deprecated(
        "Superseded by the backgroundColor val",
        ReplaceWith("backgroundColor"),
        DeprecationLevel.ERROR
    )
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
        swapBitmaps()

        val checkpoint = canvas.save()
        canvas.concat(matrix)
        bitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
        }
        canvas.restoreToCount(checkpoint)

        if (isRunning && prepareFrameFuture?.isDone != false) {
            prepareFrameFuture = executor.submit(prepareFrame)
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
        endAnimation()
        prepareFrameFuture?.cancel(false)
        prepareFrameFuture = null
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

    private fun swapBitmaps() {
        prepareFrameFuture?.let { future ->
            if (!future.isCancelled && future.isDone) {
                val nextFrame: Bitmap? = future.get()
                if (nextFrame != null) {
                    bitmapCache.release(bitmap)
                    bitmap = nextFrame
                }
                prepareFrameFuture = null
            }
        }
    }

    private fun endAnimation() {
        isRunning = false
        synchronized(state.gif) {
            state.gif.close()
            bitmapCache.flush()
        }
        unscheduleSelf(redraw)
        postAnimationEnd()
    }

    private fun postAnimationStart() {
        if (animationCallbacks.isNotEmpty()) {
            scheduleSelf(
                Runnable {
                    animationCallbacks.forEach { callback ->
                        callback.onAnimationStart(this@GifDrawable)
                    }
                },
                SystemClock.uptimeMillis()
            )
        }
    }

    private fun postAnimationEnd() {
        if (animationCallbacks.isNotEmpty()) {
            scheduleSelf(
                Runnable {
                    animationCallbacks.forEach { callback ->
                        callback.onAnimationEnd(this@GifDrawable)
                    }
                },
                SystemClock.uptimeMillis()
            )
        }
    }

    private fun shouldAnimate(): Boolean {
        if (!state.gif.isAnimated) return false

        return when (val repeatCount = loopCount) {
            is LoopCount.NoLoop -> false
            is LoopCount.Fixed -> synchronized(state) { state.loopIteration < repeatCount.count }
            is LoopCount.Infinite -> true
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

    private val prepareFrame = object : Callable<Bitmap?> {
        override fun call(): Bitmap? {
            val startTime = SystemClock.elapsedRealtime()

            val frameDelay = state.gif.currentDelay.let {
                // If the frame delay is 0, let's at last have 2 frame before we display it.
                if (it == 0L) 32L else it
            }

            synchronized(state.gif) {
                state.gif.advance()
            }
            if (state.gif.currentIndex == 0) {
                // We looped back to the first frame
                synchronized(state) {
                    state.loopIteration++
                }
            }
            // Checking if we are finished looping already
            if (!shouldAnimate()) {
                endAnimation()
                return null
            }

            val nextFrame = getCurrentFrame()
            if (nextFrame == null) {
                endAnimation()
                return null
            }
            nextFrame.prepareToDraw()

            val elapsedTime: Long = SystemClock.elapsedRealtime() - startTime
            val delay: Long = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)

            // We might have interrupted the animation at that point.
            // We let the callable finish to avoid corruption of data,
            // but it doesn't mean we need to do unnecessary calls.
            if (isRunning) {
                scheduleSelf(redraw, SystemClock.uptimeMillis() + delay)
            } else {
                synchronized(state.gif) {
                    state.gif.close()
                }
            }

            return nextFrame
        }
    }

    private val redraw = Runnable {
        invalidateSelf()
    }

    private class GifDrawableState(private val gifDescriptor: GifDescriptor) : ConstantState() {
        val gif = Gif(gifDescriptor)
        var loopCount: LoopCount? = null
        var loopIteration = 0

        override fun newDrawable(): Drawable {
            return GifDrawable(gifDescriptor.shallowClone()).also { copiedDrawable ->
                synchronized(this) {
                    copiedDrawable.state.loopCount = loopCount
                    copiedDrawable.state.loopIteration = loopIteration
                }
            }
        }

        // No need to recreate the drawable for any configurations.
        override fun getChangingConfigurations(): Int = 0
    }

    companion object {
        private val executor = Executors.newCachedThreadPool()

        /**
         * Creates a GifDrawable from an [InputStream].
         *
         * @return success if the GifDrawable was created.
         */
        fun from(inputStream: InputStream): Result<GifDrawable> =
            Parser.parse(inputStream, PixelPacking.ARGB).map(::GifDrawable)

        /**
         * Creates a GifDrawable from a [File].
         *
         * @return success if the GifDrawable was created.
         */
        fun from(file: File): Result<GifDrawable> =
            Parser.parse(file, PixelPacking.ARGB).map(::GifDrawable)
    }
}
