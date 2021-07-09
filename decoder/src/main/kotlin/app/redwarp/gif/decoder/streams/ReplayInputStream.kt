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

import java.io.InputStream

/**
 * An [InputStream] that can be "replayed" by seeking a position.
 */
abstract class ReplayInputStream : InputStream() {
    /**
     * Set the read position in the stream.
     */
    abstract fun seek(position: Int)

    /**
     * Get the current read position of the stream.
     */
    abstract fun getPosition(): Int
}
