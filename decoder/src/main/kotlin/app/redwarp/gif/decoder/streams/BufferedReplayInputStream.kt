package app.redwarp.gif.decoder.streams

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.InputStream

/**
 * A super naive implementation of a replay input stream: if we call the seek method even once,
 * then we will stop reading the original stream, and only use the in memory data.
 *
 * Check https://netty.io/4.0/api/io/netty/buffer/Unpooled.html#buffer-int-
 */

class BufferedReplayInputStream(inputStream: InputStream) : ReplayInputStream() {
    private val inputStream = inputStream.buffered()
    private val byteBuf: ByteBuf = Unpooled.buffer()
    private var replay = false

    override fun seek(position: Int) {
        byteBuf.readerIndex(position)
        replay = true
        inputStream.close()
    }

    override fun getPosition(): Int {
        return byteBuf.readerIndex()
    }

    override fun read(): Int {
        return if (!replay) {
            val read = inputStream.read()
            byteBuf.writeByte(read)
            byteBuf.readerIndex(byteBuf.readerIndex() + 1)
            read
        } else {
            byteBuf.readByte().toInt() and 0xFF
        }
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        if (!replay) {
            val readCount = inputStream.read(byteArray, offset, length)

            if (readCount > 0) {
                byteBuf.writeBytes(byteArray, offset, readCount)
            }
            byteBuf.readerIndex(byteBuf.readerIndex() + readCount)

            return readCount
        } else {
            val readCount =
                if (length > byteBuf.readableBytes()) byteBuf.readableBytes() else length

            byteBuf.readBytes(byteArray, offset, readCount)

            return readCount
        }
    }

    override fun read(byteArray: ByteArray): Int {
        if (!replay) {
            val readCount = inputStream.read(byteArray)

            if (readCount > 0) {
                byteBuf.writeBytes(byteArray)
            }
            byteBuf.readerIndex(byteBuf.readerIndex() + readCount)

            return readCount
        } else {
            val readCount =
                if (byteArray.size > byteBuf.readableBytes()) byteBuf.readableBytes() else byteArray.size

            byteBuf.readBytes(byteArray)

            return readCount
        }
    }

    override fun close() {
        inputStream.close()
    }
}