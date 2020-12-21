package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class Gif87A2ColorTest {

    @Test
    fun inputGif_gif87aHeader_returnCorrectHeader() {
        val gifFile = File("./assets/sample-2colors-87a.gif")

        val gifParser = GifParser(gifFile)

        assertEquals(GifParser.Header.GIF87a, gifParser.header)
    }

    @Test
    fun readColorMap_2colors_returnsColorsAsWhiteAndDark() {
        val gifFile = File("./assets/sample-2colors-87a.gif")

        val gifParser = GifParser(gifFile)
        assertEquals(2, gifParser.globalColorMap.size)

        assertArrayEquals(intArrayOf(0xff111111.toInt(), 0xffffffff.toInt()), gifParser.globalColorMap.colors)
    }

    @Test
    fun parseAll() {
        val gifFile = File("./assets/sample-2colors-87a.gif")

        val gifParser = GifParser(gifFile)
        gifParser.parseContent()

        val destinationDimension = gifParser.imageDescriptors[0].dimension
        val finalPixels = IntArray(destinationDimension.size)

        assertEquals(1, gifParser.imageDescriptors.size)

        gifParser.imageDescriptors[0].fillPixels(finalPixels, destinationDimension, gifParser.globalColorMap)

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
