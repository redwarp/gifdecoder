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
package app.redwarp.gif.benchmark

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

    @Before
    fun setup() {
        val inputStream = context.assets.open(GIF_FILE)
        val gifDescriptor = Parser.parse(inputStream).unwrap()
        gif = Gif(gifDescriptor)
    }

    @Test
    fun getFrame_kotlin() {
        val pixels = IntArray(gif.dimension.size)

        benchmarkRule.measureRepeated {
            gif.getFrame(0, pixels)
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
