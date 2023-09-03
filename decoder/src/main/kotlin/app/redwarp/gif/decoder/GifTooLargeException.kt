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

import app.redwarp.gif.decoder.descriptors.Dimension

/**
 * Exception raised when we detect that a GIF is simply too large for this library to decode.
 * We are using a byte array for most operation, so we are limited by java.
 */
data class GifTooLargeException(val dimension: Dimension) :
    RuntimeException(formatMessage(dimension)) {
    constructor(width: Int, height: Int) : this(Dimension(width, height))

    private companion object {
        fun formatMessage(dimension: Dimension): String {
            return "Unsupported Gif: Its dimensions ${dimension.width} x ${dimension.height}" +
                " can't be handled by the decode library."
        }
    }
}
