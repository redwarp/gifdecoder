package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class GifParserTest {

    @Test
    fun inputColorGif_gif89aHeader_returnCorrectHeader() {
        val gifFile = File("./assets/sample-2colors-89a.gif")
        val gifDescriptor = Parser.parse(gifFile)

        Assertions.assertEquals(Header.GIF89a, gifDescriptor.header)
    }

    @Test
    fun inputColorGif_gifCorruptedHeader_throwsException() {
        val gifFile = File("./assets/sample-corrupted.gif")

        assertThrows<InvalidGifException> {
            Parser.parse(gifFile)
        }
    }
}
