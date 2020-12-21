package net.redwarp.gif.decoder

import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class LwzTest {

    @Test
    fun test() {
        val data = byteArrayOf(
            0x02, 0x16, 0x8C.toByte(), 0x2D, 0x99.toByte(),
            0x87.toByte(), 0x2A, 0x1C, 0xDC.toByte(), 0x33, 0xA0.toByte(), 0x02, 0x75, 0xEC.toByte(),
            0x95.toByte(), 0xFA.toByte(), 0xA8.toByte(), 0xDE.toByte(), 0x60,
            0x8C.toByte(), 0x04, 0x91.toByte(), 0x4C, 0x01, 0x00
        )

        val buffer = ByteArrayInputStream(data).source().buffer()

        with(object : GifBuffer {}) {
            buffer.decodeLwz(100)
        }
    }

    @Test
    fun bufferTest() {
        val data = byteArrayOf(
            0x8C.toByte(), 0x2D, 0x99.toByte(),
            0x87.toByte(), 0x2A, 0x1C, 0xDC.toByte(), 0x33, 0xA0.toByte(), 0x02, 0x75, 0xEC.toByte(),
            0x95.toByte(), 0xFA.toByte(), 0xA8.toByte(), 0xDE.toByte(), 0x60,
            0x8C.toByte(), 0x04, 0x91.toByte(), 0x4C, 0x01, 0x00
        )

        val buffer: BufferedSource = ByteArrayInputStream(data).source().buffer()

        val lzwBuffer = LzwSource(2.toByte(), buffer)

        val bufferedSource = lzwBuffer.buffer()
        bufferedSource.readByteArray(10)
    }
}
