package net.redwarp.gif.decoder

import net.redwarp.gif.decoder.cleaned.LzwDecoder
import net.redwarp.gif.decoder.cleaned.Parser
import org.junit.jupiter.api.Test
import java.io.File

class NewLzwTest {
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

        lzwDecoder.decode(pixels = pixels)

        pixels.forEach {
            println(it)
        }
    }


    @Test
    fun decode_domo_properlyReturnedData() {
        val gif = Parser().parse(
            file = File("./assets/domo.gif")
        )
        val lzwDecoder = LzwDecoder(gif.imageDescriptors[0].imageData)
        val pixels = ByteArray(gif.imageDescriptors[0].dimension.size)

        lzwDecoder.decode(pixels = pixels)
    }
}