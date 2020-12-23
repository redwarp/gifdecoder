package net.redwarp.gif.decoder

import okio.buffer
import okio.source

class LzwDecoder(imageData: ByteArray) {
    private val source = imageData.inputStream().source().buffer()
    private val shadowSource = imageData.inputStream().source().buffer()
    private val lzwMinimumCodeSize = source.readByte()
    private val clear: Int = 1.shl(lzwMinimumCodeSize.toInt())
    private val endOfData: Int = clear + 1
    private var codeSize = lzwMinimumCodeSize.toInt() + 1
    private var mask: Int = (1.shl(codeSize) - 1) // For codeSize = 3, will output 0b0111

    private var stringTable: MutableList<ByteArray> = mutableListOf()
    private var bits = 0
    private var currentByte: Int = 0

    private var blockSize = 0

    init {
        for (index in 0 until clear) {
            stringTable.add(byteArrayOf(index.toByte()))
        }
        // Reserve clear and end of data
        stringTable.add(byteArrayOf())
        stringTable.add(byteArrayOf())
    }

    fun read(): Int {
        while (bits < codeSize) {
            if (blockSize == 0) {
                blockSize = source.readByte().toInt() and 0xff
            }
            currentByte += ((source.readByte().toInt() and 0xff).shl(bits))
            bits += 8
            blockSize--
        }

        val code = (currentByte and mask)
        bits -= codeSize
        currentByte = currentByte.ushr(codeSize)

        return code
    }

    /**
     * Decode the GIF LZW stream into a byte array. It will write down the color indices.
     */
    fun decode(destination: ByteArray) {
        var index = 0

        var previousString: ByteArray = ByteArray(0)

        while (true) {
            val code = read()
            var string: ByteArray
            var newEntry: ByteArray

            if (code == endOfData) {
                break
            } else if (code == clear) {
                stringTable = stringTable.take(clear + 2).toMutableList()
                codeSize = lzwMinimumCodeSize.toInt() + 1
                mask = (1.shl(codeSize) - 1)

                val first = read()
                // Write the first code
                destination[index] = stringTable[first][0]
                index++
                previousString = stringTable[first]

                continue
            } else if (code < stringTable.size) {
                // In table, we output the string from the dictionary, then add to the dictionary a new string,
                // composed of previousString + first index of string
                string = stringTable[code]

                newEntry = ByteArray(previousString.size + 1)
                previousString.copyInto(newEntry)
                newEntry[newEntry.size - 1] = string[0]
            } else {
                string = ByteArray(previousString.size + 1)
                previousString.copyInto(string)
                string[string.size - 1] = string[0]
                newEntry = string
            }
            for (color in string) {
                destination[index] = color
                index++
            }
            previousString = string

            if (stringTable.size <= 4096) {
                // We can't add a new string if the table size is bigger than 2^12
                stringTable.add(newEntry)
                if (stringTable.size - 1 == mask && codeSize < 12) {
                    // Our new entry's index is equal the the mask, we must increase the decoding code size.
                    // unless the codeSize is already maxed (12)
                    codeSize += 1
                    mask = mask.shl(1) + 1
                }
            }
        }
    }

    fun shadowDecode(destination: ByteArray, pixelCount: Int) {
        var index = 0

        var previousString: ByteArray = ByteArray(0)

        var available = clear + 2
        val prefix = ShortArray(4096)
        val suffix = ByteArray(4096)
        val pixelStack = ByteArray(4097)
        var stackTop = 0
        var first = 0

        var oldCode: Int = -1

        for (code in 0 until clear) {
            prefix[code] = 0
            suffix[code] = code.toByte()
        }
        var inCode = 0
        var pixelIndex = 0

        while (pixelIndex < pixelCount) {
            if (stackTop == 0) {
                var code = read()

                if (code > available || code == endOfData) {
                    break
                }
                if (code == clear) {
                    codeSize = lzwMinimumCodeSize.toInt() + 1
                    mask = (1.shl(codeSize) - 1)
                    available = clear + 2
                    oldCode = -1
                    continue
                }
                if (oldCode == -1) {
                    pixelStack[stackTop] = suffix[code]
                    stackTop++
                    oldCode = code
                    first = code
                    continue
                }

                inCode = code
                if (code == available) {
                    pixelStack[stackTop] = first.toByte()
                    stackTop++
                    code = oldCode
                }

                while (code > clear) {
                    pixelStack[stackTop] = suffix[code]
                    stackTop++
                    code = prefix[code].toInt()
                }

                first = suffix[code].toInt() and 0xff

                if (available >= 4096) {
                    break
                }
                pixelStack[stackTop] = first.toByte()
                stackTop++
                prefix[available] = oldCode.toShort()
                suffix[available] = first.toByte()
                available++

                if (available and mask == 0 && available < 4096) {
                    codeSize++
                    mask += available
                }
                oldCode = inCode
            }

            stackTop--
            destination[pixelIndex] = pixelStack[stackTop]
            pixelIndex++
        }
    }
}