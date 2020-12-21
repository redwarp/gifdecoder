package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.lang.UnsupportedOperationException

class GifParserTest {

    @Test
    fun inputColorGif_gif89aHeader_returnCorrectHeader() {
        val gifFile = File("./assets/sample-2colors-89a.gif")

        val gifParser = GifParser(gifFile)

        Assertions.assertEquals(GifParser.Header.GIF89a, gifParser.header)
    }

    @Test
    fun inputColorGif_gifCorruptedHeader_throwsException() {
        val gifFile = File("./assets/sample-corrupted.gif")

        assertThrows<UnsupportedOperationException> {
            GifParser(gifFile)
        }
    }
}
