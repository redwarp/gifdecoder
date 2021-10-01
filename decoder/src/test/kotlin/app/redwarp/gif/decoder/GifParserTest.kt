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

import app.redwarp.gif.decoder.descriptors.Header
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class GifParserTest {

    @Test
    fun inputColorGif_gif89aHeader_returnCorrectHeader() {
        val gifFile = File("../assets/sample-2colors-89a.gif")
        val gifDescriptor = Parser.parse(gifFile).getOrThrow()

        Assertions.assertEquals(Header.GIF89a, gifDescriptor.header)
    }

    @Test
    fun inputColorGif_gifCorruptedHeader_returnsError() {
        val gifFile = File("../assets/sample-corrupted.gif")

        Assertions.assertTrue(Parser.parse(gifFile).isFailure)
    }
}
