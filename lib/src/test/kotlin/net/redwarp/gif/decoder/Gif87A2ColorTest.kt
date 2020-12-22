package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class Gif87A2ColorTest {

    @Test
    fun inputGif_gif87aHeader_returnCorrectHeader() {
        val gifFile = File("./assets/sample-2colors-87a.gif")
        val gifDescriptor = Parser().parse(gifFile)

        assertEquals(Header.GIF87a, gifDescriptor.header)
    }

    @Test
    fun readColorMap_2colors_returnsColorsAsWhiteAndDark() {
        val gifFile = File("./assets/sample-2colors-87a.gif")
        val gifDescriptor = Parser().parse(gifFile)

        assertArrayEquals(intArrayOf(0xff111111.toInt(), 0xffffffff.toInt()), gifDescriptor.globalColorTable?.colors)
    }

    @Test
    fun parseAll() {
        val gifFile = File("./assets/sample-2colors-87a.gif")

        val gifDescriptor = Parser().parse(gifFile)
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
