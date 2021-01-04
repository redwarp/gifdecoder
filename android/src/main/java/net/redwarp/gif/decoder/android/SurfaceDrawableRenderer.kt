package net.redwarp.gif.decoder.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder

/**
 * Simple class do draw a [Drawable] on a [SurfaceHolder]
 */
class SurfaceDrawableRenderer(private val holder: SurfaceHolder, private val drawable: Drawable) :
    SurfaceHolder.Callback,
    Drawable.Callback {
    private var width: Int = 0
    private var height: Int = 0
    private var isCreated = false
    private val handler: Handler = Handler(Looper.getMainLooper())

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isCreated = true

        drawable.callback = this

        drawOnSurface()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.width = width
        this.height = height

        drawable.setBounds(0, 0, width, height)

        drawOnSurface()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawable.callback = null
        isCreated = false
    }

    private fun drawOnSurface() {
        if (isCreated) {
            val canvas =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            draw(canvas)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun invalidateDrawable(who: Drawable) {
        drawOnSurface()
    }

    private val drawRunnable = { drawOnSurface() }
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(drawRunnable, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(drawRunnable)
    }
}