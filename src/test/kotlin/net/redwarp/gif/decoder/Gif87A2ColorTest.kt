package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class Gif87A2ColorTest {

    @Test
    fun inputGif_gif87aHeader_returnCorrectHeader() {
        val gifFile = File("./src/test/resources/sample-2colors-87a.gif")

        val gifParser = GifParser(gifFile)

        assertEquals(GifParser.Header.GIF87a, gifParser.header)
    }


    @Test
    fun readColorMap_2colors_returnsColorsAsWhiteAndDark() {
        val gifFile = File("./src/test/resources/sample-2colors-87a.gif")

        val gifParser = GifParser(gifFile)
        assertEquals(2, gifParser.globalColorMap.size)

        assertArrayEquals(intArrayOf(0xff111111.toInt(), 0xffffffff.toInt()), gifParser.globalColorMap.colors)
    }
}