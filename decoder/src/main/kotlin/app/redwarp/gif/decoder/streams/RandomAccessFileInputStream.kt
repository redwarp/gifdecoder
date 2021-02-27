package net.redwarp.gif.decoder.streams

import java.io.File
import java.io.RandomAccessFile

class RandomAccessFileInputStream(val file: File) : SeekableInputStream() {
    private var _randomAccessFile: RandomAccessFile? = null
    private val randomAccessFile: RandomAccessFile
        get() {
            return _randomAccessFile ?: let {
                RandomAccessFile(file, "r").also { _randomAccessFile = it }
            }
        }

    override fun seek(position: Int) {
        randomAccessFile.seek(position.toLong())
    }

    override fun getPosition(): Int {
        return randomAccessFile.filePointer.toInt()
    }

    override fun read(): Int {
        return randomAccessFile.read()
    }

    override fun read(byteArray: ByteArray): Int {
        return randomAccessFile.read(byteArray)
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        return randomAccessFile.read(byteArray, offset, length)
    }

    override fun close() {
        randomAccessFile.close()
        _randomAccessFile = null
    }
}