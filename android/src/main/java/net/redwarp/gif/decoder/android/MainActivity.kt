package net.redwarp.gif.decoder.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<ConstraintLayout>(R.id.container)

        val earth: InputStream = assets.open("rotating_earth.gif")

        val drawable = GifDrawable(earth)

        container.background = drawable
        drawable.start()
    }
}