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
package app.redwarp.gif.decoder.utils

import java.io.InputStream

internal fun InputStream.readShortLe(): Short {
    return (read() or (read() shl 8)).toShort()
}

internal fun InputStream.readUShortLe(): UShort = readShortLe().toUShort()

internal fun InputStream.readByte(): Byte = read().toByte()

internal fun InputStream.readUByte(): UByte = read().toUByte()

internal fun InputStream.readAsciiString(byteCount: Int): String {
    val buffer = StringBuilder()
    repeat(byteCount) {
        buffer.append(read().toChar())
    }
    return buffer.toString()
}
