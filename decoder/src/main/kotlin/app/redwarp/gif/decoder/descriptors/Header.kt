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
 * There are two standard of GIF, with GIF89a being a super set of GIF87a.
 * The library is not super strict about it: It is quite frequent to find GIFs labeled as GIF87a
 * using features only available to GIF89a.
 * Sad but...
 */
enum class Header {
    GIF87a, GIF89a
}
