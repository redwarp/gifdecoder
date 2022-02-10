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

/**
 * A [ReplayInputStream] implementation wrapping a [RandomAccessFile]: replaying the stream will
 * actually set the [RandomAccessFile]'s pointer back in the file.
 * Choosing this class instead of the [BufferedReplayInputStream] is better for huge GIFs,
 * as the data will not be loaded in memory, but kept on disk.
 */
internal class RandomAccessFileInputStream(private val file: File) : ReplayInputStream() {
    private var _randomAccessFile: BufferedRandomAccessFile? = null
    private val randomAccessFile: RandomAccessFile
        get() {
            return _randomAccessFile ?: let {
                BufferedRandomAccessFile(file, "r").also { _randomAccessFile = it }
            }
        }

    override fun seek(position: Int) {
        randomAccessFile.seek(position.toLong())
    }

    override fun getPosition(): Int {
        return randomAccessFile.filePointer.toInt()
    }

    override fun shallowClone(): ReplayInputStream {
        return RandomAccessFileInputStream(file)
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
