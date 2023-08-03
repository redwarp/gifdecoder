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

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class BufferedRandomAccessFileTest {

    @Test
    fun read_regular_getData() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val file = prepareTestFile(originalData)

        val randomAccess = BufferedRandomAccessFile(file = file, bufferSize = 2)

        val readInto = ByteArray(128)
        val count = randomAccess.read(readInto)

        assertEquals(4, count)
        assertArrayEquals(originalData, readInto.sliceArray(0 until count))
    }

    @Test
    fun read_afterSeek_getData() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val file = prepareTestFile(originalData)

        val randomAccess = BufferedRandomAccessFile(file = file, bufferSize = 2)
        randomAccess.seek(2)

        val readInto = ByteArray(128)
        val count = randomAccess.read(readInto)

        assertEquals(2, count)
        assertArrayEquals(originalData.sliceArray(2 until 4), readInto.sliceArray(0 until count))
    }

    @Test
    fun read_multipleReadAndSeek_getData() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val file = prepareTestFile(originalData)
        val randomAccess = BufferedRandomAccessFile(file = file, bufferSize = 2)

        val readInto1 = ByteArray(128)
        val readInto2 = ByteArray(128)

        randomAccess.seek(2)
        val count1 = randomAccess.read(readInto1)

        assertEquals(2, count1)
        assertArrayEquals(byteArrayOf(0x03, 0x04), readInto1.sliceArray(0 until count1))

        randomAccess.seek(2)
        val count2 = randomAccess.read(readInto2)
        assertEquals(2, count2)

        assertArrayEquals(readInto1, readInto2)
    }

    @Test
    fun read_moreThanLengthOfFile_readLengthOfFile() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val file = prepareTestFile(originalData)
        val inputStream = RandomAccessFileInputStream(file = file, bufferSize = 2)

        val readInto = ByteArray(128)

        val read = inputStream.read(readInto, 0, 5)

        assertEquals(4, read)
    }

    @Test
    fun reads_inTwoBuffers() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val file = prepareTestFile(originalData)
        val randomAccess = BufferedRandomAccessFile(file = file, bufferSize = 2)

        val readInto1 = ByteArray(2)
        val readInto2 = ByteArray(2)

        randomAccess.read(readInto1)
        randomAccess.read(readInto2)

        val joined = readInto1 + readInto2

        assertArrayEquals(originalData, joined)
    }

    @Test
    fun skip_canSkipWithCountBiggerThanTotalSize() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val file = prepareTestFile(originalData)
        val randomAccess = RandomAccessFileInputStream(file = file, bufferSize = 2)

        assertEquals(4, randomAccess.skip(5))
    }

    private fun prepareTestFile(data: ByteArray): File {
        val file = File.createTempFile("data", null)
        file.deleteOnExit()

        file.writeBytes(data)
        return file
    }
}
