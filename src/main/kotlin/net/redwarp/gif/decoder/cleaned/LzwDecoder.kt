package net.redwarp.gif.decoder.cleaned

import okio.buffer
import okio.source

private const val TRANSPARENT_COLOR = 0x00000000

class LzwDecoder(imageData: ByteArray) {
    private val source = imageData.inputStream().source().buffer()
    private val lzwMinimumCodeSize = source.readByte()
    private val clear: Int = 1.shl(lzwMinimumCodeSize.toInt())
    private val endOfData: Int = clear + 1
    private var codeSize = lzwMinimumCodeSize.toInt() + 1
    private var mask: Int = (1.shl(codeSize) - 1) // For codeSize = 3, will output 0b0111

    private var dictionary: MutableList<ByteArray> = mutableListOf()
    private var bits = 0
    private var currentByte: Int = 0

    private var blockSize = 0

    init {
        for (index in 0 until clear) {
            dictionary.add(byteArrayOf(index.toByte()))
        }
        // Reserve clear and end of data
        dictionary.add(byteArrayOf())
        dictionary.add(byteArrayOf())
    }

    fun read(): Int {
        if (bits < codeSize) {
            if (blockSize == 0) {
                blockSize = source.readByte().toInt()
            }
            currentByte += ((source.readByte().toInt() and 0xff).shl(bits))
            bits += 8
            blockSize--
        }

        val code = (currentByte and mask)
        bits -= codeSize
        currentByte = currentByte.ushr(codeSize)

        if (code == clear) {
            codeSize = lzwMinimumCodeSize.toInt() + 1
            mask = (1.shl(codeSize) - 1)
        } else if (code == mask) {
            // Increase code size
            codeSize += 1
            mask = mask.shl(1) + 1
        }

        return code
    }

    fun decode(pixels: ByteArray) {
        var index = 0
        var previousString: ByteArray? = null

        while (true) {
            val code = read()
            if (code == endOfData) {
                break
            } else if (code == clear) {
                dictionary = dictionary.take(clear + 2).toMutableList()
                previousString = null
            } else if (previousString == null) {
                val string = dictionary[code]
                for (color in string) {
                    pixels[index] = color
                    index++
                }
                previousString = string
            } else if (code < dictionary.size) {
                // In table
                val string = dictionary[code]
                for (color in string) {
                    pixels[index] = color
                    index++
                }
                val newEntry = ByteArray(previousString.size + 1)
                previousString.copyInto(newEntry)
                newEntry[newEntry.size - 1] = string[0]
                dictionary.add(newEntry)
                previousString = string
            } else if (code >= dictionary.size) {
                val string = ByteArray(previousString.size + 1)
                previousString.copyInto(string)
                string[string.size - 1] = string[0]
                for (color in string) {
                    pixels[index] = color
                    index++
                }
                dictionary.add(string)
                previousString = string
            }
        }
    }

    fun decode(pixels: IntArray, colorTable: ColorTable, transparentColorIndex: Byte?) {
        var index = 0
        var previousString: ByteArray? = null

        while (true) {
            val code = read()
            if (code == endOfData) {
                break
            } else if (code == clear) {
                dictionary = dictionary.take(clear + 2).toMutableList()
                previousString = null
            } else if (previousString == null) {
                val string = dictionary[code]
                for (colorIndex in string) {
                    if (colorIndex == transparentColorIndex) {
                        // We don't write transparent color
                        index++
                    } else {
                        pixels[index] = colorTable.colors[colorIndex.toInt()]
                        index++
                    }
                }
                previousString = string
            } else if (code < dictionary.size) {
                // In table
                val string = dictionary[code]
                for (colorIndex in string) {
                    if (colorIndex == transparentColorIndex) {
                        // We don't write transparent color
                        index++
                    } else {
                        pixels[index] = colorTable.colors[colorIndex.toInt()]
                        index++
                    }
                }
                val newEntry = ByteArray(previousString.size + 1)
                previousString.copyInto(newEntry)
                newEntry[newEntry.size - 1] = string[0]
                dictionary.add(newEntry)
                previousString = string
            } else if (code >= dictionary.size) {
                val string = ByteArray(previousString.size + 1)
                previousString.copyInto(string)
                string[string.size - 1] = string[0]
                for (colorIndex in string) {
                    if (colorIndex == transparentColorIndex) {
                        // We don't write transparent color
                        index++
                    } else {
                        pixels[index] = colorTable.colors[colorIndex.toInt()]
                        index++
                    }
                }
                dictionary.add(string)
                previousString = string
            }
        }
    }
}