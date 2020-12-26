package net.redwarp.gif.decoder.android

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import net.redwarp.gif.decoder.LoopCount
import net.redwarp.gif.decoder.lzw.NativeLzwDecoder
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var drawable: GifDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val gifStream: InputStream = assets.open("glasses-aspect_ratio.gif")
        drawable = GifDrawable(gifStream)
        drawable.setLoopCount(LoopCount.Infinite)

        imageView.setImageDrawable(drawable)

        val imageData = ByteArray(10) { 1 }
        val destination = ByteArray(10)

        val decoder = NativeLzwDecoder()

    }

    override fun onResume() {
        super.onResume()
        drawable.start()
    }

    override fun onPause() {
        super.onPause()
        drawable.stop()
    }
}
