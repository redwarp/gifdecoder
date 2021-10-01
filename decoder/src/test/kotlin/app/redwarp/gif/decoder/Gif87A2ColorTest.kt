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

import app.redwarp.gif.decoder.descriptors.Header
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class Gif87A2ColorTest {

    @Test
    fun inputGif_gif87aHeader_returnCorrectHeader() {
        val gifFile = File("../assets/sample-2colors-87a.gif")
        val gifDescriptor = Parser.parse(gifFile).getOrThrow()

        assertEquals(Header.GIF87a, gifDescriptor.header)
    }

    @Test
    fun readColorMap_2colors_returnsColorsAsWhiteAndDark() {
        val gifFile = File("../assets/sample-2colors-87a.gif")
        val gifDescriptor = Parser.parse(gifFile).getOrThrow()

        assertArrayEquals(
            intArrayOf(0xff111111.toInt(), 0xffffffff.toInt()),
            gifDescriptor.globalColorTable
        )
    }

    @Test
    fun parseAll() {
        val gifFile = File("../assets/sample-2colors-87a.gif")

        val gifDescriptor = Parser.parse(gifFile).getOrThrow()
        val gif = Gif(gifDescriptor)

        val destinationDimension = gif.dimension
        val finalPixels = IntArray(destinationDimension.size)

        assertEquals(1, gif.frameCount)

        gif.getFrame(0, finalPixels)

        val expected = intArrayOf(
            0xFF111111.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF111111.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt()
        )
        assertArrayEquals(expected, finalPixels)
    }
}
