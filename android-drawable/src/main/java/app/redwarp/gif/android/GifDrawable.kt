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
import app.redwarp.gif.android.tasks.Cancellable
import app.redwarp.gif.android.tasks.CancellingPoolExecutor
import app.redwarp.gif.decoder.Gif
import app.redwarp.gif.decoder.Parser
import app.redwarp.gif.decoder.descriptors.GifDescriptor
import app.redwarp.gif.decoder.descriptors.params.LoopCount
import app.redwarp.gif.decoder.descriptors.params.PixelPacking
import java.io.File
import java.io.InputStream
import java.nio.IntBuffer
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
class GifDrawable(gifDescriptor: GifDescriptor) : Drawable(), Animatable2Compat, Runnable {
    private val state = GifDrawableState(gifDescriptor)
    private val bitmapCache = BitmapCache()
    private val animationCallbacks = mutableListOf<Animatable2Compat.AnimationCallback>()
    private val gifWidth get() = state.gif.dimension.width
    private val gifHeight get() = state.gif.dimension.height
    private val pixels = IntArray(state.gif.dimension.size)
    private val pixelsBuffer = IntBuffer.wrap(pixels)
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private val matrix = Matrix()

    private var isRunning: AtomicBoolean = AtomicBoolean(false)
    private var prepareFrameFuture: Future<*>? = null
    private var frameTime: AtomicLong = AtomicLong(0)
    private val transparencies: Array<Boolean?> = arrayOfNulls(gifDescriptor.imageDescriptors.size)
    private var bitmap: Bitmap? = getFrame(0)
        set(value) {
            field.let(bitmapCache::release)
            field = value
        }
    private var nextBitmap: Bitmap? = null
    private var nextIndex: Int = 0

    private val bitmapLock: Any = Any()

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    /**
     * Gets or overrides the normal maximum loop count of the gif.
     * Doing so will also reset the current loop count to zero.
     *
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
        DeprecationLevel.ERROR,
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
        val checkpoint = canvas.save()
        canvas.concat(matrix)
        synchronized(bitmapLock) {
            bitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
            }
        }
        canvas.restoreToCount(checkpoint)
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
    }

    @Deprecated("This method is no longer used in graphics optimizations", ReplaceWith(""))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        if (isRunning()) return // Already running

        isRunning.set(true)
        nextFrame(true)

        postAnimationStart()
    }

    override fun stop() {
        if (!isRunning.get()) return // Already stopped.

        isRunning.set(false)
        cancelNextFrame()

        postAnimationEnd()
    }

    override fun isRunning(): Boolean = isRunning.get()

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)

        if (changed) {
            if (visible) {
                nextFrame(true)
            } else {
                cancelNextFrame()
            }
        }

        return changed
    }

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
            Matrix.ScaleToFit.FILL,
        )
    }

    override fun run() {
        nextFrame(false)
    }

    override fun mutate(): Drawable {
        return constantState.newDrawable()
    }

    private fun postAnimationStart() {
        if (animationCallbacks.isNotEmpty()) {
            scheduleSelf(
                {
                    animationCallbacks.forEach { callback ->
                        callback.onAnimationStart(this@GifDrawable)
                    }
                },
                SystemClock.uptimeMillis(),
            )
        }
    }

    private fun postAnimationEnd() {
        if (animationCallbacks.isNotEmpty()) {
            scheduleSelf(
                {
                    animationCallbacks.forEach { callback ->
                        callback.onAnimationEnd(this@GifDrawable)
                    }
                },
                SystemClock.uptimeMillis(),
            )
        }
    }

    private fun shouldAnimate(loopIteration: Int): Boolean {
        if (!isVisible || !isRunning()) return false
        synchronized(state) {
            if (!state.gif.isAnimated) return false
        }

        return when (val repeatCount = loopCount) {
            is LoopCount.NoLoop -> false
            is LoopCount.Fixed -> synchronized(state) { loopIteration < repeatCount.count }
            is LoopCount.Infinite -> true
        }
    }

    private fun getFrame(index: Int): Bitmap? {
        if (state.gif.getFrame(index, pixels).isFailure) return null

        val frame: Bitmap = bitmapCache.obtain(
            width = gifWidth,
            height = gifHeight,
            config = Bitmap.Config.ARGB_8888,
        )

        pixelsBuffer.position(0)
        frame.copyPixelsFromBuffer(pixelsBuffer)
        return frame
    }

    private fun nextFrame(unschedule: Boolean) {
        if (unschedule) {
            unscheduleSelf(this)
        }

        synchronized(bitmapLock) {
            nextBitmap?.let { nextBitmap ->
                bitmap = nextBitmap

                synchronized(state) {
                    state.frameIndex = nextIndex

                    if (nextIndex == 0) {
                        // We looped back to the first frame
                        state.loopIteration++
                    }
                }
            }
            nextBitmap = null
        }
        invalidateSelf()

        // Check what would be the loop count if we advanced the frame counter
        val iteration = if (state.gif.currentIndex == state.gif.frameCount - 1) {
            state.loopIteration + 1
        } else {
            state.loopIteration
        }
        // Checking if we are finished looping already
        if (shouldAnimate(iteration)) {
            prepareFrameFuture = executor.submit(prepareNextFrame())
        } else {
            bitmapCache.flush()
        }
    }

    private fun cancelNextFrame() {
        prepareFrameFuture?.cancel(false)
        prepareFrameFuture = null
        unscheduleSelf(this)
        synchronized(bitmapLock) {
            bitmapCache.release(nextBitmap)
            nextBitmap = null
        }
    }

    private fun prepareNextFrame(): Cancellable<Unit> = object : Cancellable<Unit>() {
        override fun call() {
            val currentFrameDelay = state.gif.getDelay(state.frameIndex).let {
                // If the frame delay is 0, let's at last have 2 frame before we display it.
                // It's what most web browsers do.
                if (it == 0L) 32L else it
            }

            val nextIndex = synchronized(state) {
                (state.frameIndex + 1) % state.gif.frameCount
            }

            val nextFrame = getFrame(nextIndex) ?: return
            nextFrame.prepareToDraw()
            synchronized(bitmapLock) {
                nextBitmap?.let(bitmapCache::release)
                nextBitmap = nextFrame
                this@GifDrawable.nextIndex = nextIndex
            }

            val optimalTime = frameTime.get() + currentFrameDelay
            val actualTime = if (optimalTime < SystemClock.uptimeMillis()) {
                SystemClock.uptimeMillis()
            } else {
                optimalTime
            }
            frameTime.set(actualTime)

            if (isCancelled) return

            // Scheduling next draw and next frame decode.
            // Queuing message with scheduling should respect the order:
            // the redraw message should be treated before the update.
            scheduleSelf(this@GifDrawable, actualTime)
        }
    }

    private class GifDrawableState(private val gifDescriptor: GifDescriptor) : ConstantState() {
        val gif = Gif(gifDescriptor)
        var loopCount: LoopCount? = null
        var loopIteration = 0
        var frameIndex: Int = 0

        override fun newDrawable(): Drawable {
            return GifDrawable(gifDescriptor.shallowClone()).also { copiedDrawable ->
                synchronized(this) {
                    copiedDrawable.state.loopCount = loopCount
                    copiedDrawable.state.loopIteration = loopIteration
                    copiedDrawable.state.frameIndex = frameIndex
                }
            }
        }

        // No need to recreate the drawable for any configurations.
        override fun getChangingConfigurations(): Int = 0
    }

    companion object {
        private val executor = CancellingPoolExecutor()

        /**
         * Creates a GifDrawable from an [InputStream].
         *
         * @return success if the GifDrawable was created.
         */
        fun from(inputStream: InputStream): Result<GifDrawable> =
            Parser.parse(inputStream, PixelPacking.ABGR).map(::GifDrawable)

        /**
         * Creates a GifDrawable from a [File].
         *
         * @return success if the GifDrawable was created.
         */
        fun from(file: File): Result<GifDrawable> =
            Parser.parse(file, PixelPacking.ABGR).map(::GifDrawable)
    }
}
