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
    private val drawRunnable = { drawOnSurface() }

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
        handler.post(drawRunnable)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(what)
    }
}
