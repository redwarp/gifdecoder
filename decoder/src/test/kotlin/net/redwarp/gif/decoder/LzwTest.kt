package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.io.File

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
    fun parseSampleData_shadowMatchOriginal() {
        val lzwDecoder = LzwDecoder(sampleData)
        val expected = ByteArray(15)

        lzwDecoder.decode(expected)

        val shadowLzwDecoder = LzwDecoder2()
        val shadow = ByteArray(15)
        shadowLzwDecoder.decode(sampleData, shadow, 15)

        assertArrayEquals(expected, shadow)
    }

    @Test
    fun parseSampleData_properlyReturnedIndex() {
        val lzwDecoder = LzwDecoder(sampleData)
        repeat(9) {
            val code = lzwDecoder.read()
            println(code.toUByte().toString(2))
        }
    }

    @Test
    fun decode_properlyReturnedData() {
        val lzwDecoder = LzwDecoder(sampleData)
        val pixels = ByteArray(15)

        lzwDecoder.decode(destination = pixels)

        val expected = byteArrayOf(0, 2, 2, 2, 0, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1)

        assertArrayEquals(expected, pixels)
    }


    @Test
    fun decode_domo_properlyReturnedData() {
        val gif = Parser.parse(
            file = File("./assets/domo.gif")
        )
        val lzwDecoder = LzwDecoder(gif.imageDescriptors[0].imageData)
        val pixels = ByteArray(gif.imageDescriptors[0].dimension.size)

        lzwDecoder.decode(destination = pixels)
    }
}