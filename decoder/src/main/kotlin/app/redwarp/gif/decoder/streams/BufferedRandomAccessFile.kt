/* Copyright 2020 Benoit Vermont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.redwarp.gif.decoder.streams

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class BufferedRandomAccessFile(
    private val file: File,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : Closeable {
    private val buffer: ByteArray = ByteArray(bufferSize)
    private var bufferEnd = 0
    private var bufferPosition = 0

    /**
     * The position inside the actual file.
     */
    private var realPosition: Long = 0

    private val leftOverInBuffer get() = bufferEnd - bufferPosition

    @Throws(IOException::class)
    fun read(): Int {
        if (bufferPosition >= bufferEnd && fillBuffer() < 0) {
            return -1
        }
        return if (bufferEnd == 0) {
            -1
        } else {
            val value = buffer[bufferPosition].toInt() and 0xFF
            bufferPosition += 1
            value
        }
    }

    /**
     * Reads the next [bufferSize] bytes into the internal buffer.
     *
     * @return The total number of bytes read into the buffer, or -1 if there is no more data
     * because the end of the file has been reached.
     * @throws IOException If the first byte cannot be read for any reason other than end of file,
     * or if the random access file has been closed, or if some other I/O error occurs.
     */
    @Throws(IOException::class)
    private fun fillBuffer(): Int {
        RandomAccessFile(file, "r").use { randomAccess ->
            randomAccess.seek(realPosition)
            val readCount = randomAccess.read(buffer, 0, bufferSize)
            if (readCount >= 0) {
                realPosition += readCount.toLong()
                bufferEnd = readCount
                bufferPosition = 0
            }
            return readCount
        }
    }

    @Throws(IOException::class)
    private fun fillBuffer(randomAccessFile: RandomAccessFile): Int {
        randomAccessFile.seek(realPosition)
        val readCount = randomAccessFile.read(buffer, 0, bufferSize)
        if (readCount >= 0) {
            realPosition += readCount.toLong()
            bufferEnd = readCount
            bufferPosition = 0
        }
        return readCount
    }

    @Throws(IOException::class)
    fun read(b: ByteArray): Int = read(b, 0, b.size)

    @Throws(IOException::class)
    fun read(b: ByteArray, off: Int, len: Int): Int {
        var leftToRead = len
        var currentOffset = off
        var copied = 0

        val writeToBuff = {
            val toWrite = minOf(leftOverInBuffer, leftToRead)
            System.arraycopy(buffer, bufferPosition, b, currentOffset, toWrite)
            copied += toWrite
            bufferPosition += toWrite
            currentOffset += toWrite
            leftToRead -= toWrite
        }

        writeToBuff()
        if (leftToRead == 0) {
            return copied
        }

        RandomAccessFile(file, "r").use { randomAccessFile ->
            while (true) {
                if (fillBuffer(randomAccessFile) > 0) {
                    writeToBuff()

                    if (leftToRead == 0) {
                        return copied
                    }
                } else {
                    return copied
                }
            }
        }
    }

    val filePointer: Long
        @Throws(IOException::class)
        get() {
            return realPosition - bufferEnd + bufferPosition
        }

    @Throws(IOException::class)
    fun seek(pos: Long) {
        val n = (realPosition - pos).toInt()
        if (n in 0..bufferEnd) {
            bufferPosition = bufferEnd - n
        } else {
            bufferEnd = 0
            bufferPosition = 0
            realPosition = pos
        }
    }

    override fun close() {
        bufferEnd = 0
        bufferPosition = 0
        realPosition = 0
    }
}
