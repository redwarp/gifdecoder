package net.redwarp.gif.decoder

import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout

class LzwSource(private val lzwCodeSize: Byte, private val wrapped: BufferedSource) : Source {
    private var currentCodeSize = lzwCodeSize + 1
    private var currentMask = 1.shl(currentCodeSize) - 1
    private var bitRead = 0
    private val resetCode = 1.shl(lzwCodeSize.toInt())

    private var currentCodes: Int = wrapped.readByte().toInt() and 0xff or (wrapped.readByte().toInt() and 0xff shl 8)

    override fun close() {
        wrapped.close()
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        var read = 0L

        for (count in 0 until byteCount) {
            val value = currentCodes and currentMask
            bitRead += currentCodeSize
            currentCodes = currentCodes.ushr(currentCodeSize)
            while (bitRead >= 8) {
                currentCodes = currentCodes or (wrapped.readByte().toInt() and 0xff shl (16 - bitRead))
                bitRead -= 8
            }
            if (value >= currentMask - 1) {
                currentCodeSize += 1
                currentMask = currentMask.shl(1) + 1
            }
            if (value == resetCode) {
                currentCodeSize = lzwCodeSize + 1
                currentMask = 1.shl(currentCodeSize) - 1
            }

            read++
            sink.writeByte(value)
        }
        return read
    }

    override fun timeout(): Timeout {
        return wrapped.timeout()
    }
}
