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
package app.redwarp.gif.decoder.lzw

private const val MAX_STACK_SIZE = 4096

/**
 * A lzw decode tailored for the GIFs specificities.
 */
internal class LzwDecoder {
    private val prefix = ShortArray(MAX_STACK_SIZE)
    private val suffix = ByteArray(MAX_STACK_SIZE)
    private val pixelStack = ByteArray(MAX_STACK_SIZE + 1)

    fun decode(imageData: ByteArray, destination: ByteArray, pixelCount: Int) {
        val prefix = prefix
        val suffix = suffix
        val pixelStack = pixelStack

        var dataIndex = 0

        val lzwMinimumCodeSize = imageData[dataIndex]
        dataIndex++

        val clear: Int = 1.shl(lzwMinimumCodeSize.toInt())
        val endOfData: Int = clear + 1
        var codeSize = lzwMinimumCodeSize.toInt() + 1

        var bits = 0
        var currentByte = 0
        var blockSize = 0
        var mask = (1.shl(codeSize) - 1) // For codeSize = 3, will output 0b0111

        var available = clear + 2
        var stackTop = 0
        var first = 0

        var oldCode: Int = -1

        for (code in 0 until clear) {
            prefix[code] = 0
            suffix[code] = code.toByte()
        }
        var pixelIndex = 0

        while (pixelIndex < pixelCount) {
            // Getting the next code
            while (bits < codeSize) {
                if (blockSize == 0) {
                    blockSize = imageData[dataIndex].toInt() and 0xff
                    dataIndex++
                }
                currentByte += ((imageData[dataIndex].toInt() and 0xff).shl(bits))
                dataIndex++
                bits += 8
                blockSize--
            }

            var code = (currentByte and mask)
            bits -= codeSize
            currentByte = currentByte.ushr(codeSize)

            // Interpreting the code
            if (code == clear) {
                codeSize = lzwMinimumCodeSize.toInt() + 1
                mask = (1.shl(codeSize) - 1)
                available = clear + 2
                oldCode = -1
                continue
            } else if (code > available || code == endOfData) {
                break
            } else if (oldCode == -1) {
                destination[pixelIndex] = suffix[code]
                pixelIndex++
                oldCode = code
                first = code
                continue
            }

            val initialCode = code
            if (code >= available) {
                pixelStack[stackTop] = first.toByte()
                stackTop++
                code = oldCode
            }

            while (code >= clear) {
                pixelStack[stackTop] = suffix[code]
                stackTop++
                code = prefix[code].toInt() and 0xffff
            }

            first = suffix[code].toInt() and 0xff

            destination[pixelIndex] = first.toByte()
            pixelIndex++

            while (stackTop > 0) {
                stackTop--
                destination[pixelIndex] = pixelStack[stackTop]
                pixelIndex++
            }

            if (available < MAX_STACK_SIZE) {
                prefix[available] = oldCode.toShort()
                suffix[available] = first.toByte()
                available++
                if (available and mask == 0 && available < MAX_STACK_SIZE) {
                    codeSize++
                    mask += available
                }
            }
            oldCode = initialCode
        }
    }
}
