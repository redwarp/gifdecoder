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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RandomAccessGifTest {

    @Test
    fun fileDeleted_getFrameWithProvidedIntArray_ReturnsFailure() {
        val gifFile = File("../assets/domo.gif")
        val tempFile = File.createTempFile("test", "gif")
        gifFile.copyTo(tempFile, true)

        val gif = Gif.from(tempFile).getOrThrow()

        val intArray = IntArray(gif.dimension.size)

        assertTrue(gif.getFrame(0, intArray).isSuccess)

        tempFile.delete()

        assertTrue(gif.getFrame(1, intArray).isFailure)
    }

    @Test
    fun fileDeleted_getFrameReturnsFailure() {
        val gifFile = File("../assets/domo.gif")
        val tempFile = File.createTempFile("test", "gif")
        gifFile.copyTo(tempFile, true)

        val gif = Gif.from(tempFile).getOrThrow()

        assertTrue(gif.getFrame(0).isSuccess)

        tempFile.delete()

        assertTrue(gif.getFrame(1).isFailure)
    }
}
