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

import app.redwarp.gif.decoder.streams.ReplayInputStream

/**
 * Describe the gif after parsing it once: contains header, global color tables,
 * the image descriptors for each frames, as well as a reference to the full input stream.
 */
class GifDescriptor(
    val header: Header,
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: IntArray?,
    val loopCount: Int?,
    val imageDescriptors: List<ImageDescriptor>,
    val data: ReplayInputStream
) {
    fun shallowClone(): GifDescriptor {
        return GifDescriptor(
            header = header,
            logicalScreenDescriptor = logicalScreenDescriptor,
            globalColorTable = globalColorTable,
            loopCount = loopCount,
            imageDescriptors = imageDescriptors,
            data = data.shallowClone()
        )
    }

    protected fun finalize() {
        data.close()
    }
}
