package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.descriptors.Header
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class GifParserTest {

    @Test
    fun inputColorGif_gif89aHeader_returnCorrectHeader() {
        val gifFile = File("../assets/sample-2colors-89a.gif")
        val gifDescriptor = Parser.parse(gifFile).unwrap()

        Assertions.assertEquals(Header.GIF89a, gifDescriptor.header)
    }

    @Test
    fun inputColorGif_gifCorruptedHeader_returnsError() {
        val gifFile = File("../assets/sample-corrupted.gif")

        Assertions.assertTrue(Parser.parse(gifFile) is Result.Error)
    }
}
