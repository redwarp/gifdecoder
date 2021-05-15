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
package app.redwarp.gif.decoder.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import app.redwarp.gif.android.GifDrawable

class GifWrapperDrawable(private val gifDrawable: GifDrawable) : Drawable() {
    private var backgroundColor = gifDrawable.backgroundColor()
    private val paint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    private val matrix = Matrix()

    private val chainingCallback: Callback = object : Callback {
        override fun invalidateDrawable(who: Drawable) {
            invalidateSelf()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            scheduleSelf(what, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            unscheduleSelf(what)
        }
    }

    init {
        gifDrawable.callback = chainingCallback
    }

    fun setBackgroundColor(color: Int) {
        backgroundColor = color
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        if (backgroundColor == Color.TRANSPARENT) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            canvas.drawPaint(paint)
        }
        val checkpoint = canvas.save()
        canvas.concat(matrix)
        gifDrawable.draw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        gifDrawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        gifDrawable.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int {
        return gifDrawable.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return gifDrawable.intrinsicHeight
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        matrix.setRectToRect(
            gifDrawable.bounds.toRectF(),
            bounds.toRectF(),
            Matrix.ScaleToFit.CENTER
        )
    }

    private fun Rect.toRectF(): RectF =
        RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}
