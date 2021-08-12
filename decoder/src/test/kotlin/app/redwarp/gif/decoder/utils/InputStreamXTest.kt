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
package app.redwarp.gif.decoder.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputStreamXTest {

    @Test
    fun readShortLe() {
        val inputStream = byteArrayOf(0b00000011, 0b10000000.toByte()).inputStream()
        val read = inputStream.readShortLe()

        val expected: Short = -32765
        val expectedBinary: Short = 0b1000000000000011.toShort()

        assertEquals(expected, read)
        assertEquals(expectedBinary, read)
    }

    @Test
    fun readUShortLe() {
        val inputStream = byteArrayOf(0b00000011, 0b10000000.toByte()).inputStream()
        val read = inputStream.readUShortLe()

        val expected: UShort = 32771u
        val expectedBinary: UShort = 0b1000000000000011u

        assertEquals(expected, read)
        assertEquals(expectedBinary, read)
    }

    @Test
    fun readByte() {
        val inputStream = byteArrayOf(0b10001000.toByte()).inputStream()
        val read = inputStream.readByte()

        val expected: Byte = -120

        assertEquals(expected, read)
    }

    @Test
    fun readUByte() {
        val inputStream = byteArrayOf(0b10001000.toByte()).inputStream()
        val read = inputStream.readUByte()

        val expected: UByte = 136u

        assertEquals(expected, read)
    }

    @Test
    fun readAsciiString() {
        val inputStream = byteArrayOf(104, 101, 108, 108, 111).inputStream()
        val read = inputStream.readAsciiString(5)

        val expected = "hello"
        assertEquals(expected, read)
    }
}
