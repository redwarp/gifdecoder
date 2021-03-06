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

data class GraphicControlExtension(
    val disposalMethod: Disposal,
    val delayTime: UShort,
    val transparentColorIndex: Byte?
) {
    enum class Disposal {
        NOT_SPECIFIED,
        DO_NOT_DISPOSE, // Keep the previous frame in place.
        RESTORE_TO_BACKGROUND, // After the image is shown, restore the whole canvas to the background color.
        RESTORE_TO_PREVIOUS // After these pixels are shown, restore the canvas to the previous image.
    }
}
