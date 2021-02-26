package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.lzw.LzwDecoder
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class LzwTest {
    private val sampleData = byteArrayOf(
        // Initial code size 2
        0b00000010,
        // Length  5
        0b00000101,
        // Data
        0b10000100.toByte(),
        0b01101110,
        0b00100111,
        0b11000001.toByte(),
        0b01011101,
    )

    @Test
    fun decode_properlyReturnedData() {
        val lzwDecoder = LzwDecoder()
        val pixels = ByteArray(15)

        lzwDecoder.decode(imageData = sampleData, destination = pixels, pixelCount = 15)

        val expected = byteArrayOf(0, 2, 2, 2, 0, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1)

        assertArrayEquals(expected, pixels)
    }
}
