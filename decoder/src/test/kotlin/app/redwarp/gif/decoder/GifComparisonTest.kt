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

import com.madgag.gif.fmsware.GifDecoder
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream

class GifComparisonTest {
    @Test
    fun validateAllGifsInAssetsFolder() {
        val directory = File("../assets")
        val gifFiles = directory.listFiles { file ->
            file.extension == "gif"
        }
        runCatching {
            gifFiles?.forEach { gifFile ->
                val gifDescriptor = Parser.parse(gifFile).getOrThrow()
                println("Checking ${gifFile.name}")
                val gif = Gif(gifDescriptor)
                val decoder = GifDecoder()
                decoder.read(FileInputStream(gifFile))
                val frameCount = decoder.frameCount
                assert(frameCount == gif.frameCount)

                for (index in 0 until frameCount) {
                    val frameFromExternalLibrary = decoder.getFrame(index)
                    val ownFrame = gif.getFrameAsBufferedImage(index)

                    assertImagesAreSame(frameFromExternalLibrary, ownFrame)
                }
            }
        }
    }

    private fun Gif.getFrameAsBufferedImage(index: Int): BufferedImage {
        val pixels = IntArray(dimension.size)
        getFrame(index, pixels)
        val image =
            BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, dimension.width, dimension.height, pixels, 0, dimension.width)
        return image
    }

    private fun assertImagesAreSame(first: BufferedImage, second: BufferedImage) {
        assert(first.width == second.width) {
            "Width are different: ${first.width} vs ${second.width}"
        }

        assert(first.height == second.height) {
            "Width are different: ${first.height} vs ${second.height}"
        }

        for (x in 0 until first.width) {
            for (y in 0 until first.height) {
                val pixel1 = first.getRGB(x, y)
                val pixel2 = second.getRGB(x, y)
                assert(pixel1 == pixel2) {
                    "Pixel at [$x,$y] are different: $pixel1 vs $pixel2"
                }
            }
        }
    }
}
