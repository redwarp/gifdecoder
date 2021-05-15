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

import java.io.File
import java.io.RandomAccessFile

internal class RandomAccessFileInputStream(private val file: File) : ReplayInputStream() {
    private var _randomAccessFile: RandomAccessFile? = null
    private val randomAccessFile: RandomAccessFile
        get() {
            return _randomAccessFile ?: let {
                RandomAccessFile(file, "r").also { _randomAccessFile = it }
            }
        }

    override fun seek(position: Int) {
        randomAccessFile.seek(position.toLong())
    }

    override fun getPosition(): Int {
        return randomAccessFile.filePointer.toInt()
    }

    override fun read(): Int {
        return randomAccessFile.read()
    }

    override fun read(byteArray: ByteArray): Int {
        return randomAccessFile.read(byteArray)
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        return randomAccessFile.read(byteArray, offset, length)
    }

    override fun close() {
        randomAccessFile.close()
        _randomAccessFile = null
    }
}
