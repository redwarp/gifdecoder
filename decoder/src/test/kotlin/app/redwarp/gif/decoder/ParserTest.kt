package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.streams.BufferedReplayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ParserTest {
    @Test
    fun readImageData() {
        val data = byteArrayOf(
            2, // LZW Minimum Code
            5, // Block size
            1, 2, 3, 4, 5,
            3, // Block size
            1, 2, 3,
            0 // Terminator
        )
        val bufferedSource = BufferedReplayInputStream(data.inputStream())

        with(Parser) {
            val imageData = bufferedSource.readImageData()
            assertEquals(0, imageData.position)
            assertEquals(12, imageData.length)
        }
    }

    @Test
    fun parse_properCountOfImageDescriptors() {
        val gifFile = File("../assets/domo.gif")

        val gif = Parser.parse(gifFile).unwrap()

        assertEquals(3, gif.imageDescriptors.size)
    }
}
