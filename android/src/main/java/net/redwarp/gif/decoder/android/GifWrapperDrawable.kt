package net.redwarp.gif.decoder.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import net.redwarp.gif.android.GifDrawable

class GifWrapperDrawable(private val gifDrawable: GifDrawable) : Drawable() {
    private var backgroundColor = gifDrawable.backgroundColor()
    private val paint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

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
        gifDrawable.draw(canvas)
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

        val width = right - left
        val height = bottom - top

        val gifWidth = gifDrawable.intrinsicWidth
        val gifHeight = gifDrawable.intrinsicHeight

        val ratio = width.toFloat() / height.toFloat()
        val gifRatio = gifWidth.toFloat() / gifHeight.toFloat()

        if (ratio > gifRatio) {
            val gifTop = 0
            val gifBottom = height
            val newWidth = (height * gifRatio).toInt()
            val gifLeft = (width - newWidth) / 2
            val gifRight = newWidth + (width - newWidth) / 2
            gifDrawable.setBounds(gifLeft, gifTop, gifRight, gifBottom)
        } else {
            val gifLeft = 0
            val gifRight = width
            val newHeight = (width / gifRatio).toInt()
            val gifTop = (height - newHeight) / 2
            val gifBottom = newHeight + (height - newHeight) / 2
            gifDrawable.setBounds(gifLeft, gifTop, gifRight, gifBottom)
        }
    }
}