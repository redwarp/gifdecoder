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

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import app.redwarp.gif.android.GifDrawable
import app.redwarp.gif.decoder.descriptors.params.LoopCount

class MainActivity : AppCompatActivity() {

    private val drawables: MutableList<Animatable2Compat> = mutableListOf()
    private lateinit var surfaceDrawableRenderer: SurfaceDrawableRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawables.clear()

        val imageView1 = findViewById<ImageView>(R.id.imageView1)
        val drawable = GifDrawable.from(assets.open("full_colour.gif")).getOrThrow()
        drawable.loopCount = LoopCount.Infinite
        drawables.add(drawable)
        imageView1.setImageDrawable(drawable)
        drawable.start()
        imageView1.setOnClickListener {
            if (drawable.isRunning) {
                drawable.stop()
            } else {
                drawable.start()
            }
        }

        val imageView2 = findViewById<ImageView>(R.id.imageView2)
        val drawable2 = GifDrawable.from(assets.open("derpy_cat.gif")).getOrThrow()
        imageView2.setImageDrawable(drawable2)
        drawables.add(drawable2)
        drawable2.start()
        imageView2.setOnClickListener {
            if (drawable2.isRunning) {
                drawable2.stop()
            } else {
                drawable2.start()
            }
        }

        val imageView3 = findViewById<ImageView>(R.id.imageView3)
        val glassesFile = assets.open("glasses-aspect_ratio.gif").toFile(this, "glasses.gif")
        val drawable3 = GifDrawable.from(glassesFile).getOrThrow()
        imageView3.setImageDrawable(drawable3)
        drawables.add(drawable3)
        drawable3.start()
        imageView3.setOnClickListener {
            if (drawable3.isRunning) {
                drawable3.stop()
            } else {
                drawable3.start()
            }
        }

        val imageView4 = findViewById<ImageView>(R.id.imageView4)
        val drawable4 = GifDrawable.from(assets.open("domo-interlaced.gif")).getOrThrow()
        imageView4.setImageDrawable(drawable4)
        drawables.add(drawable4)
        drawable4.start()
        imageView4.setOnClickListener {
            if (drawable4.isRunning) {
                drawable4.stop()
            } else {
                drawable4.start()
            }
        }

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        val drawable5 = drawable3.constantState.newDrawable() as GifDrawable
        drawable5.loopCount = LoopCount.Infinite
        val wrapper = GifWrapperDrawable(drawable5)
        wrapper.setBackgroundColor(Color.WHITE)
        surfaceDrawableRenderer = SurfaceDrawableRenderer(surfaceView.holder, wrapper)
        drawables.add(drawable5)
        drawable5.start()
        surfaceView.setOnClickListener {
            if (drawable5.isRunning) {
                drawable5.stop()
            } else {
                drawable5.start()
            }
        }
        drawable5.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationStart(drawable: Drawable?) {
                Toast.makeText(this@MainActivity, "Animation started", Toast.LENGTH_SHORT).show()
            }

            override fun onAnimationEnd(drawable: Drawable?) {
                Toast.makeText(this@MainActivity, "Animation stopped", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
