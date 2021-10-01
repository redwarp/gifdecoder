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
package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.descriptors.Dimension
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import javax.imageio.ImageIO

class GifTest {
    @Test
    fun backgroundColor_withTransparentGif_returnsTransparent() {
        val gifDescriptor = Parser.parse(File("../assets/domo.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(0x00000000, gif.backgroundColor)
    }

    @Test
    fun backgroundColor_gifWithWhiteBackground_returnsWhite() {
        val gifDescriptor = Parser.parse(File("../assets/sample-2colors-87a.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(0xffffffff.toInt(), gif.backgroundColor)
    }

    @Test
    fun isAnimated_withAnimatedGif_returnsTrue() {
        val gifDescriptor = Parser.parse(File("../assets/domo.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(true, gif.isAnimated)
    }

    @Test
    fun isAnimated_gifWithSingleFrame_returnsFalse() {
        val gifDescriptor = Parser.parse(File("../assets/sample-2colors-87a.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(false, gif.isAnimated)
    }

    @Test
    fun dimension_returnsProperGifDimension() {
        val gifDescriptor = Parser.parse(File("../assets/domo.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(Dimension(width = 19, height = 23), gif.dimension)
    }

    @Test
    fun frameCount_dimension_returnsProperFrameCount() {
        val gifDescriptor = Parser.parse(File("../assets/domo.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)

        assertEquals(3, gif.frameCount)
    }

    @Test
    fun getFrame_each_properlyRenders() {
        val gifDescriptor = Parser.parse(File("../assets/domo.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension

        val pixels = IntArray(dimension.size)
        for (index in 0 until gif.frameCount) {
            gif.getFrame(index, pixels)
            val expectedPixels = loadExpectedPixels(File("../assets/frames/domo_$index.png"))

            assertArrayEquals(expectedPixels, pixels)
        }
    }

    @Test
    fun getFrame_interlaced_properlyRenders() {
        val gifDescriptor = Parser.parse(File("../assets/domo-interlaced.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension

        val pixels = IntArray(dimension.size)
        for (index in 0 until gif.frameCount) {
            gif.getFrame(index, pixels)
            val expectedPixels = loadExpectedPixels(File("../assets/frames/domo_$index.png"))

            assertArrayEquals(expectedPixels, pixels)
        }
    }

    @Test
    fun getFrame_nonAnimatedGif_properlyRenders() {
        val gifDescriptor = Parser.parse(File("../assets/sunflower.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension
        val pixels = IntArray(dimension.size)

        gif.getFrame(0, pixels)
        val expectedPixels = loadExpectedPixels(File("../assets/frames/sunflower_0.png"))

        assertArrayEquals(expectedPixels, pixels)
    }

    @Test
    fun getFrame_lastFrameDirectly_properlyRenders() {

        val gifDescriptor = Parser.parse(File("../assets/domo-interlaced.gif")).getOrThrow()
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension
        val pixels = IntArray(dimension.size)

        gif.getFrame(2, pixels)
        val expectedPixels = loadExpectedPixels(File("../assets/frames/domo_2.png"))

        assertArrayEquals(expectedPixels, pixels)
    }

    @Test
    fun getFrameDelay_animatedWithZeroDelaySpecified_returns0() {
        val gif = Gif.from(File("../assets/butterfly.gif")).getOrThrow()

        assertEquals(0L, gif.currentDelay)
    }

    @Test
    fun getFrameDelay_animatedWithDelaySpecified_returnsDelay() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        assertEquals(100L, gif.currentDelay)
    }

    @Test
    fun getRatio_gifWith1To1Ratio_returns1() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()
        val aspectRatio = gif.aspectRatio

        assertEquals(1.0, aspectRatio, 0.0001)
    }

    @Test
    fun getRatio_gifWith3To1Ratio_returns3Something() {
        val gif = Gif.from(File("../assets/glasses-aspect_ratio.gif")).getOrThrow()
        val aspectRatio = gif.aspectRatio

        assertTrue { aspectRatio > 3.0 && aspectRatio < 3.2 }
    }

    @Test
    fun advance_2times_currentIndexChanged() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        assertEquals(0, gif.currentIndex)

        repeat(2) {
            gif.advance()
        }

        assertEquals(2, gif.currentIndex)
    }

    @Test
    fun advance_enoughToLoop_currentIndexChanged() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        assertEquals(0, gif.currentIndex)

        repeat(3) {
            gif.advance()
        }

        assertEquals(0, gif.currentIndex)
    }

    @Test
    fun getCurrentFrame_multipleCalls_sameResult() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        gif.advance()

        val firstCall = IntArray(gif.dimension.size)
        val secondCall = IntArray(gif.dimension.size)
        gif.getCurrentFrame(firstCall)
        gif.getCurrentFrame(secondCall)

        assertArrayEquals(firstCall, secondCall)
    }

    @Test
    fun previousIndex_current0_returnLastIndex() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        with(gif) {
            assertEquals(2, 0.previousIndex)
        }
    }

    @Test
    fun previousIndex_current1_returns0() {
        val gif = Gif.from(File("../assets/domo.gif")).getOrThrow()

        with(gif) {
            assertEquals(0, 1.previousIndex)
        }
    }

    private fun loadExpectedPixels(file: File): IntArray {
        val input = ImageIO.read(file)
        val image = BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.drawImage(input, 0, 0, input.width, input.height, null)
        graphics.dispose()
        return (image.raster.dataBuffer as DataBufferInt).data
    }
}
