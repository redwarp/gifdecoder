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

import app.redwarp.gif.decoder.streams.BufferedReplayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ParserTest {
    @Test
    fun readImageData() {
        val data = byteArrayOf(
            2, // LZW Minimum Code
            5, // Block size
            1, 2, 3, 4, 5,
            3, // Block size
            1, 2, 3,
            0 // Terminator
        )
        val bufferedSource = BufferedReplayInputStream(data.inputStream())

        with(Parser) {
            val imageData = bufferedSource.readImageData()
            assertEquals(0, imageData.position)
            assertEquals(12, imageData.length)
        }
    }

    @Test
    fun parse_properCountOfImageDescriptors() {
        val gifFile = File("../assets/domo.gif")

        val gif = Parser.parse(gifFile).getOrThrow()

        assertEquals(3, gif.imageDescriptors.size)
    }
}
