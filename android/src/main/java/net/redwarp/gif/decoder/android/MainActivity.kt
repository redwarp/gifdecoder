package net.redwarp.gif.decoder.android

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import net.redwarp.gif.android.GifDrawable

class MainActivity : AppCompatActivity() {

    private val drawables: MutableList<Animatable2Compat> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawables.clear()

        val imageView = findViewById<ImageView>(R.id.imageView)
        val drawable = GifDrawable.from(assets.open("full_colour.gif"))
        drawable.setRepeatCount(GifDrawable.REPEAT_INFINITE)
        drawables.add(drawable)
        imageView.setImageDrawable(drawable)
        drawable.start()
        imageView.setOnClickListener {
            if(drawable.isRunning){
                drawable.stop()
            } else {
                drawable.start()
            }
        }

        val imageView2 = findViewById<ImageView>(R.id.imageView2)
        val drawable2 = GifDrawable.from(assets.open("derpy_cat.gif"))
        imageView2.setImageDrawable(drawable2)
        drawables.add(drawable2)
        drawable2.start()
        imageView2.setOnClickListener {
            if(drawable2.isRunning){
                drawable2.stop()
            } else {
                drawable2.start()
            }
        }

        val imageView3 = findViewById<ImageView>(R.id.imageView3)
        val drawable3 = GifDrawable.from(assets.open("glasses-aspect_ratio.gif"))
        imageView3.setImageDrawable(drawable3)
        drawables.add(drawable3)
        drawable3.start()
        imageView3.setOnClickListener {
            if(drawable3.isRunning){
                drawable3.stop()
            } else {
                drawable3.start()
            }
        }

        val imageView4 = findViewById<ImageView>(R.id.imageView4)
        val drawable4 = GifDrawable.from(assets.open("domo-interlaced.gif"))
        imageView4.setImageDrawable(drawable4)
        drawables.add(drawable4)
        drawable4.start()
        imageView4.setOnClickListener {
            if(drawable4.isRunning){
                drawable4.stop()
            } else {
                drawable4.start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        drawables.forEach(Animatable2Compat::start)
    }

    override fun onPause() {
        drawables.forEach(Animatable2Compat::stop)
        super.onPause()
    }
}
