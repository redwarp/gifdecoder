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
 * Load and keep the whole [InputStream] in memory, should be avoided for huge GIFs.
 */
internal class BufferedReplayInputStream private constructor(
    inputStream: InputStream?,
    private var loadedData: ByteArray?,
    private var state: State
) : ReplayInputStream() {
    constructor(inputStream: InputStream) : this(inputStream, null, State())
    private constructor(loadedData: ByteArray, state: State) : this(null, loadedData, state)

    private var reader: Reader? = inputStream?.let { Reader(it) }

    override fun seek(position: Int) {
        close()
        this.state.position = position
    }

    override fun getPosition(): Int {
        return reader?.size() ?: state.position
    }

    override fun read(): Int {
        return reader?.read()
            ?: (requireNotNull(loadedData)[state.position].toInt() and 0xFF).also {
                state.position += 1
            }
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        reader.let { reader ->
            if (reader != null) {
                return reader.read(byteArray, offset, length)
            } else {
                val readCount =
                    if (length > readableBytes()) readableBytes() else length

                requireNotNull(loadedData).copyInto(
                    destination = byteArray,
                    destinationOffset = 0,
                    startIndex = state.position + offset,
                    endIndex = state.position + offset + readCount
                )
                state.position += readCount

                return readCount
            }
        }
    }

    override fun read(byteArray: ByteArray): Int {
        reader.let { reader ->
            if (reader != null) {
                return reader.read(byteArray)
            } else {
                val readCount =
                    if (byteArray.size > readableBytes()) readableBytes() else byteArray.size

                requireNotNull(loadedData).copyInto(
                    destination = byteArray,
                    destinationOffset = 0,
                    startIndex = state.position,
                    endIndex = state.position + readCount
                )

                state.position += readCount

                return readCount
            }
        }
    }

    @Synchronized
    override fun close() {
        if (loadedData != null) return

        reader?.let { reader ->
            reader.readAll()
            loadedData = reader.toByteArray().also {
                reader.close()
                this.reader = null
            }
        }
    }

    override fun shallowClone(): ReplayInputStream {
        close()
        return BufferedReplayInputStream(requireNotNull(loadedData), state.copy())
    }

    private fun readableBytes(): Int {
        return loadedData?.let {
            it.size - state.position
        } ?: 0
    }

    private data class State(var position: Int = 0)

    private class Reader(inputStream: InputStream) : AutoCloseable {
        private val inputStream = inputStream.buffered()
        private var outputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        private var totalCount = 0

        fun size(): Int {
            return outputStream.size()
        }

        override fun close() {
            inputStream.close()
            outputStream.close()
        }

        fun read(): Int {
            return inputStream.read().also {
                outputStream.write(it)
                totalCount++
            }
        }

        fun read(byteArray: ByteArray): Int {
            val readCount = inputStream.read(byteArray)

            if (readCount > 0) {
                outputStream.write(byteArray, 0, readCount)
            }
            totalCount += readCount

            return readCount
        }

        fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
            val readCount = inputStream.read(byteArray, offset, length)

            if (readCount > 0) {
                outputStream.write(byteArray, offset, readCount)
            }
            totalCount += readCount

            return readCount
        }

        fun readAll(): Long {
            return inputStream.copyTo(outputStream)
        }

        fun toByteArray(): ByteArray {
            return outputStream.toByteArray()
        }
    }
}
