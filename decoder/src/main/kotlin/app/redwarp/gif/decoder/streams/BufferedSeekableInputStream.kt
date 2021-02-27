package net.redwarp.gif.decoder.streams

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.InputStream

// Check https://netty.io/4.0/api/io/netty/buffer/Unpooled.html#buffer-int-
class BufferedSeekableInputStream(inputStream: InputStream) : SeekableInputStream() {
    val inputStream = inputStream.buffered()

    private val byteBuf: ByteBuf = Unpooled.buffer()

    override fun seek(position: Int) {

        byteBuf.readerIndex(position)
    }

    override fun getPosition(): Int {
        return byteBuf.readerIndex()
    }

    override fun read(): Int {
        return if (byteBuf.readerIndex() >= byteBuf.writerIndex()) {
            val read = inputStream.read()
            byteBuf.writeByte(read)
            byteBuf.readerIndex(byteBuf.readerIndex() + 1)
            read
        } else {
            byteBuf.readByte().toInt() and 0xFF
        }
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        if (byteBuf.readerIndex() >= byteBuf.writerIndex()) {
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
        if (byteBuf.readerIndex() >= byteBuf.writerIndex()) {
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