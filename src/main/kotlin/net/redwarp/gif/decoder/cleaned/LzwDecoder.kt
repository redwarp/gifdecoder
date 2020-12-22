package net.redwarp.gif.decoder.cleaned

import okio.buffer
import okio.source

class LzwDecoder(imageData: ByteArray) {
    private val source = imageData.inputStream().source().buffer()
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

        println("Code size: $codeSize")
    }
}