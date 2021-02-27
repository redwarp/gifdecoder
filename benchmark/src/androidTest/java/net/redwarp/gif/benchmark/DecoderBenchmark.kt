package net.redwarp.gif.benchmark

import android.content.Context
import android.graphics.Bitmap
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.redwarp.gif.decoder.Gif
import app.redwarp.gif.decoder.Parser
import com.bumptech.glide.gifdecoder.SimpleBitmapProvider
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import net.redwarp.gif.decoder.NativeGif
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val GIF_FILE = "glasses-aspect_ratio.gif"

@RunWith(AndroidJUnit4::class)
class DecoderBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var gif: Gif
    private lateinit var nativeGif: NativeGif

    @Before
    fun setup() {
        val inputStream = context.assets.open(GIF_FILE)
        val gifDescriptor = Parser.parse(inputStream)
        gif = Gif(gifDescriptor)
        nativeGif = NativeGif(gifDescriptor)
    }

    @Test
    fun getFrame_kotlin() {
        val pixels = IntArray(gif.dimension.size)

        benchmarkRule.measureRepeated {
            gif.getFrame(0, pixels)
        }
    }

    @Test
    fun getFrame_native() {
        val pixels = IntArray(gif.dimension.size)

        benchmarkRule.measureRepeated {
            nativeGif.getFrame(0, pixels)
        }
    }

    @Test
    fun getFrame_withFinalBitmap_kotlin() {
        val pixels = IntArray(gif.dimension.size)
        val (width, height) = gif.dimension
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        benchmarkRule.measureRepeated {
            gif.getFrame(0, pixels)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    @Test
    fun getFrame_withFinalBitmap_native() {
        val pixels = IntArray(gif.dimension.size)
        val (width, height) = gif.dimension
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        benchmarkRule.measureRepeated {
            nativeGif.getFrame(0, pixels)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    @Test
    fun getFrame_withFinalBitmap_glideDecoder() {
        val data = context.assets.open(GIF_FILE).readBytes()
        val standardGifDecoder = StandardGifDecoder(SimpleBitmapProvider())
        standardGifDecoder.read(data)
        standardGifDecoder.advance()

        benchmarkRule.measureRepeated {
            standardGifDecoder.nextFrame
        }
    }
}
