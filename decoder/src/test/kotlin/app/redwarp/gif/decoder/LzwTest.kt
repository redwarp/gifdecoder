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
package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.lzw.LzwDecoder
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class LzwTest {
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
    fun decode_properlyReturnedData() {
        val lzwDecoder = LzwDecoder()
        val pixels = ByteArray(15)

        lzwDecoder.decode(imageData = sampleData, destination = pixels, pixelCount = 15)

        val expected = byteArrayOf(0, 2, 2, 2, 0, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1)

        assertArrayEquals(expected, pixels)
    }
}
