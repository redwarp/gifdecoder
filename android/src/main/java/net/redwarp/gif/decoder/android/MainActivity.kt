package net.redwarp.gif.decoder.android

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import net.redwarp.gif.decoder.LoopCount
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private val drawables: MutableList<GifDrawable> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val gifStream: InputStream = assets.open("full_colour.gif")
        val drawable = GifDrawable(gifStream)
        drawable.setLoopCount(LoopCount.Infinite)
        drawables.add(drawable)
        imageView.setImageDrawable(drawable)

        // val imageView2 = findViewById<ImageView>(R.id.imageView2)
        // val drawable2 = GifDrawable(assets.open("derpy_cat.gif"))
        // imageView2.setImageDrawable(drawable2)
        // drawables.add(drawable2)

        // val imageView3 = findViewById<ImageView>(R.id.imageView3)
        // val drawable3 = GifDrawable(assets.open("glasses-aspect_ratio.gif"))
        // imageView3.setImageDrawable(drawable3)
        // drawables.add(drawable3)

        // val imageView4 = findViewById<ImageView>(R.id.imageView4)
        // val drawable4 = GifDrawable(assets.open("domo-interlaced.gif"))
        // imageView4.setImageDrawable(drawable4)
        // drawables.add(drawable4)
    }

    override fun onResume() {
        super.onResume()
        drawables.forEach(GifDrawable::start)
    }

    override fun onPause() {
        super.onPause()
        drawables.forEach(GifDrawable::stop)
    }
}
