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
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder

/**
 * Simple class do draw a [Drawable] on a [SurfaceHolder]
 */
class SurfaceDrawableRenderer(holder: SurfaceHolder, private val drawable: Drawable) :
    SurfaceHolder.Callback,
    Drawable.Callback {
    private var width: Int = 0
    private var height: Int = 0
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val drawRunnable = Runnable { surface?.let(this::drawOnSurface) }
    private var surface: Surface? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawable.callback = this
        holder.surface?.let {
            surface = it
            drawOnSurface(it)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.width = width
        this.height = height

        drawable.setBounds(0, 0, width, height)

        holder.surface?.let {
            surface = it
            drawOnSurface(it)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawable.callback = null
        surface?.release()
        surface = null
    }

    private fun drawOnSurface(surface: Surface) {
        val canvas =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        draw(canvas)

        surface.unlockCanvasAndPost(canvas)
    }

    private fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun invalidateDrawable(who: Drawable) {
        handler.post(drawRunnable)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(what)
    }
}
