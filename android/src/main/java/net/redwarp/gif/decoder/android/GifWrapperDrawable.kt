package net.redwarp.gif.decoder.android

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
import net.redwarp.gif.android.GifDrawable

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