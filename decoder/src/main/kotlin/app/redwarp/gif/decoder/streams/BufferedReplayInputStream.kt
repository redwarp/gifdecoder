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

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * A super naive implementation of a replay input stream: if we call the seek method even once,
 * then we will stop reading the original stream, and only use the in memory data.
 */
internal class BufferedReplayInputStream(inputStream: InputStream) : ReplayInputStream() {
    private val inputStream = inputStream.buffered()
    private val outputStream = ByteArrayOutputStream()
    private var position = 0
    private var totalCount = 0
    private var replay = false

    private var _loadedData: ByteArray? = null
    private val loadedData: ByteArray
        get() {
            return _loadedData ?: outputStream.toByteArray().also(this::_loadedData::set)
        }

    override fun seek(position: Int) {
        replay = true
        if (_loadedData == null) {
            _loadedData = outputStream.toByteArray()
            outputStream.close()
        }
        this.position = position
        inputStream.close()
    }

    override fun getPosition(): Int {
        return if (!replay) outputStream.size()
        else {
            position
        }
    }

    override fun read(): Int {
        return if (!replay) {
            val read = inputStream.read()
            outputStream.write(read)
            totalCount++
            read
        } else {
            (loadedData[position].toInt() and 0xFF).also {
                position += 1
            }
        }
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        if (!replay) {
            val readCount = inputStream.read(byteArray, offset, length)

            if (readCount > 0) {
                outputStream.write(byteArray, offset, readCount)
            }
            totalCount += readCount

            return readCount
        } else {
            val readCount =
                if (length > readableBytes()) readableBytes() else length

            loadedData.copyInto(
                destination = byteArray,
                destinationOffset = 0,
                startIndex = position + offset,
                endIndex = position + offset + readCount
            )
            position += readCount

            return readCount
        }
    }

    override fun read(byteArray: ByteArray): Int {
        if (!replay) {
            val readCount = inputStream.read(byteArray)

            if (readCount > 0) {
                outputStream.write(byteArray, 0, readCount)
            }
            totalCount += readCount

            return readCount
        } else {
            val readCount =
                if (byteArray.size > readableBytes()) readableBytes() else byteArray.size

            loadedData.copyInto(
                destination = byteArray,
                destinationOffset = 0,
                startIndex = position,
                endIndex = position + readCount
            )

            position += readCount

            return readCount
        }
    }

    override fun close() {
        inputStream.close()
    }

    private fun readableBytes(): Int {
        val myByteArray = _loadedData
        return if (myByteArray == null) {
            0
        } else {
            myByteArray.size - position
        }
    }
}
