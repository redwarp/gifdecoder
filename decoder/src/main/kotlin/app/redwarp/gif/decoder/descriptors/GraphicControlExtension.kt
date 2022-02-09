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
 * The Graphic Control Extension contains parameters used when processing a graphic rendering block.
 */
data class GraphicControlExtension(
    val disposalMethod: Disposal,
    val delayTime: UShort,
    val transparentColorIndex: Byte?
) {
    /**
     * Disposal method of the frame, A.K.A. what to do after a frame has been rendered.
     */
    enum class Disposal {
        /**
         * Not specified.
         */
        NOT_SPECIFIED,

        /**
         * Keep the frame in place. Future frames will be drawn on top.
         */
        DO_NOT_DISPOSE,

        /**
         * After the frame is shown, restore the whole canvas to the background color.
         */
        RESTORE_TO_BACKGROUND,

        /**
         * After the frame is shown, restore the canvas to the previous frame.
         */
        RESTORE_TO_PREVIOUS
    }
}
