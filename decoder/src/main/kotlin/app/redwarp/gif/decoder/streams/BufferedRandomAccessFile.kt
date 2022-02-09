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

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * A kotlin version of the BufferedRandomAccessFile java version from Apache, found here:
 * [BufferedRandomAccessFile](https://github.com/apache/pdfbox/blob/a27ee91/fontbox/src/main/java/org/apache/fontbox/ttf/BufferedRandomAccessFile.java)
 */
class BufferedRandomAccessFile(
    file: File,
    mode: String,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) :
    RandomAccessFile(file, mode) {
    private val buffer: ByteArray = ByteArray(bufferSize)
    private var bufferEnd = 0
    private var bufferPosition = 0

    /**
     * The position inside the actual file.
     */
    private var realPosition: Long = 0

    @Throws(IOException::class)
    override fun read(): Int {
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
        val readCount = super.read(buffer, 0, bufferSize)
        if (readCount >= 0) {
            realPosition += readCount.toLong()
            bufferEnd = readCount
            bufferPosition = 0
        }
        return readCount
    }

    /**
     * Clears the local buffer.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun invalidate() {
        bufferEnd = 0
        bufferPosition = 0
        realPosition = super.getFilePointer()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var currentLength = len // length of what is left to read (shrinks)
        var currentOffset = off // offset where to put read data (grows)
        var totalRead = 0
        while (true) {
            val leftover = bufferEnd - bufferPosition
            if (currentLength <= leftover) {
                System.arraycopy(buffer, bufferPosition, b, currentOffset, currentLength)
                bufferPosition += currentLength
                return totalRead + currentLength
            }
            // currentLength > leftover, we need to read more than what remains in buffer
            System.arraycopy(buffer, bufferPosition, b, currentOffset, leftover)
            totalRead += leftover
            bufferPosition += leftover
            if (fillBuffer() > 0) {
                currentOffset += leftover
                currentLength -= leftover
            } else {
                return if (totalRead == 0) {
                    -1
                } else totalRead
            }
        }
    }

    @Throws(IOException::class)
    override fun getFilePointer(): Long {
        return realPosition - bufferEnd + bufferPosition
    }

    @Throws(IOException::class)
    override fun seek(pos: Long) {
        val n = (realPosition - pos).toInt()
        if (n in 0..bufferEnd) {
            bufferPosition = bufferEnd - n
        } else {
            super.seek(pos)
            invalidate()
        }
    }
}
