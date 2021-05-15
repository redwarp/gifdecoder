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

class BufferedReplayInputStreamTest {

    @Test
    fun read_regular_getData() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val bufferedReplayInputStream = BufferedReplayInputStream(originalData.inputStream())

        val readInto = ByteArray(128)
        val count = bufferedReplayInputStream.read(readInto)

        assertEquals(4, count)
        assertArrayEquals(originalData, readInto.sliceArray(0 until count))
    }

    @Test
    fun read_thenSetPositionAndRead_readDataFromPosition() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val bufferedReplayInputStream = BufferedReplayInputStream(originalData.inputStream())

        val readInto = ByteArray(128)
        bufferedReplayInputStream.read(readInto)

        bufferedReplayInputStream.seek(2)

        val count = bufferedReplayInputStream.read(readInto)

        assertEquals(2, count)
        assertArrayEquals(
            originalData.sliceArray(2 until originalData.size),
            readInto.sliceArray(0 until count)
        )
    }

    @Test
    fun getPosition_initialRead_getAccuratePosition() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val bufferedReplayInputStream = BufferedReplayInputStream(originalData.inputStream())

        val readInto = ByteArray(128)
        bufferedReplayInputStream.read(readInto, 0, 2)

        assertEquals(2, bufferedReplayInputStream.getPosition())
    }

    @Test
    fun getPosition_afterSeekAndRead_getAccuratePosition() {
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val bufferedReplayInputStream = BufferedReplayInputStream(originalData.inputStream())

        val readInto = ByteArray(128)
        bufferedReplayInputStream.read(readInto)
        bufferedReplayInputStream.seek(2)
        bufferedReplayInputStream.read()

        assertEquals(3, bufferedReplayInputStream.getPosition())
    }
}
