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
package app.redwarp.gif.decoder.descriptors

/**
 * Representation of the dimension of a gif or a frame.
 */
data class Dimension(val width: Int, val height: Int) {
    constructor(width: UShort, height: UShort) : this(width.toInt(), height.toInt())

    val size = width * height

    /**
     * As we use one big [ByteArray] to deserialize a picture, we can't handle an image with more
     * than around [Int.MAX_VALUE] items.
     * It is an approximation to return early.
     */
    val isSupported: Boolean
        get() {
            val pixelCount: Long = width.toLong() * height.toLong()
            return pixelCount < Int.MAX_VALUE.toLong() - 10
        }
}
