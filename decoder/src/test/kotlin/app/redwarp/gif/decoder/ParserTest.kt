package app.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertArrayEquals
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
        val bufferedSource = data.inputStream()

        with(Parser) {
            val copied = bufferedSource.readImageData()
            assertArrayEquals(data, copied)
        }
    }

    @Test
    fun parse_properCountOfImageDescriptors() {
        val gifFile = File("../assets/domo.gif")

        val gif = Parser.parse(gifFile)

        assertEquals(3, gif.imageDescriptors.size)
    }
}
