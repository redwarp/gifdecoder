package net.redwarp.gif.decoder

import okio.Buffer
import okio.BufferedSource

/**
 * max decoder pixel stack size
 */
private const val MAX_STACK_SIZE = 4096

private const val MASK_INT_LOWEST_BYTE = 0x000000FF
private const val NULL_CODE = -1
private const val COLOR_TRANSPARENT_BLACK = 0x00000000

interface GifBuffer {
    fun BufferedSource.readHeader(): GifParser.Header {
        return when (val header = readByteString(6L).string(Charsets.US_ASCII)) {
            "GIF87a" -> GifParser.Header.GIF87a
            "GIF89a" -> GifParser.Header.GIF89a
            else -> throw UnsupportedOperationException("Unsupported header type: $header")
        }
    }

    fun BufferedSource.readGlobalColorMap(packedField: Int): GifParser.ColorTable {
        skip(1) // We can skip pixel aspect ratio.

        val hasGlobalMap = (0b1000_0000 and packedField) == 0b1000_0000

        return if (hasGlobalMap) {
            readColorTable(packedField)
        } else {
            val pixelMask = 0b0000_0111
            val pixelCount = pixelMask and packedField

            val size = 1.shl(pixelCount + 1)
            Palettes.createFakeColorMap(size)
        }
    }

    private fun BufferedSource.readColorTable(packedField: Int): GifParser.ColorTable {
        val pixelMask = 0b0000_0111
        val pixelCount = pixelMask and packedField

        val size = 1.shl(pixelCount + 1)

        val colors = IntArray(size)
        for (colorIndex in 0 until size) {
            val r = readByte().toUInt()
            val g = readByte().toUInt()
            val b = readByte().toUInt()

            val color: UInt = 0xff000000u or r.shl(16) or g.shl(8) or b
            colors[colorIndex] = color.toInt()
        }

        return GifParser.ColorTable(size, colors)
    }

    fun BufferedSource.decodeLwz(pixelCount: Int): Buffer {
        val lzwCodeSize = readByte()
        val block = Buffer()

        val result = Buffer()

        var blockCount = readByte().toLong()
        while (blockCount != 0L) {
            read(block, blockCount)
            blockCount = readByte().toLong()
        }

        val clear = 1.shl(lzwCodeSize.toInt())
        val endOfInformation = clear + 1
        val table = ByteArray(MAX_STACK_SIZE)
        for (color in 0 until clear) {
            table[color] = color.toByte()
        }
        table[clear] = clear.toByte()
        table[endOfInformation] = endOfInformation.toByte()
        var nextTableEntry = endOfInformation + 1

        val currentCodeSize = lzwCodeSize + 1

        return result
    }

    fun BufferedSource.lwzMightWork() {
        val offset = Point(readShortLe(), readShortLe())
        val dimension = Dimension(readShortLe(), readShortLe())

        val mapInfo = readByte().toInt()
        val mapMask: Int = 0b1000_0000
        val usesLocalMap = mapInfo and mapMask == mapMask
        val interlaced = mapInfo and 0b0100_0000 == 0b0100_0000

        val localMap: GifParser.ColorTable? = if (usesLocalMap) readColorTable(mapInfo) else null

        var code: Int = 0
        var old: Int = 0

        val lzwCodeSize = readByte()
        val block = ByteArray(256)

        var blockCount = readByte().toInt()
        while (blockCount != 0) {
            read(block, 0, blockCount)
        }
    }

    fun BufferedSource.readImageDescriptor(graphicControl: GraphicControl?): ImageDescriptor {
        val offset = Point(readShortLe(), readShortLe())
        val dimension = Dimension(readShortLe(), readShortLe())

        val mapInfo = readByte().toInt()
        val mapMask: Int = 0b1000_0000
        val usesLocalMap = (mapInfo and mapMask) == mapMask
        val interlaced = (mapInfo and 0b0100_0000) == 0b0100_0000

        val localMap: GifParser.ColorTable? = if (usesLocalMap) readColorTable(mapInfo) else null

        // Weird territory
        val block = ByteArray(256)

        val nullCode = -1
        val npix: Int = dimension.width * dimension.height

        var available: Int
        var codeMask: Int
        var codeSize: Int
        var endOfInformation: Int
        var inCode: Int
        var oldCode: Int
        var bits: Int = 0
        var code: Int
        var count: Int = 0
        var i: Int = 0
        var datum: Int = 0
        var first: Int = 0
        var top: Int = 0
        var bi: Int = 0
        var pi: Int = 0

        val dstPixels = ByteArray(npix)

        val prefix = ShortArray(MAX_STACK_SIZE) { 0 }
        val suffix = ByteArray(MAX_STACK_SIZE) { it.toByte() }
        val pixelStack = ByteArray(MAX_STACK_SIZE + 1)

        val dataSize = readByte().toInt()
        val clear = 1.shl(dataSize)
        endOfInformation = clear + 1
        available = clear + 2
        oldCode = nullCode

        codeSize = dataSize + 1
        codeMask = (1 shl codeSize) - 1

        // Decode GIF pixel stream.
        while (i < npix) {
            // Read a new data block.
            if (count == 0) {
                count = readBlock(block)
                if (count <= 0) {
                    break
                }
                bi = 0
            }
            datum += block[bi].toInt() and MASK_INT_LOWEST_BYTE shl bits
            bits += 8
            ++bi
            --count
            while (bits >= codeSize) {
                // Get the next code.
                code = datum and codeMask
                datum = datum shr codeSize
                bits -= codeSize

                // Interpret the code.
                if (code == clear) {
                    // Reset decoder.
                    codeSize = dataSize + 1
                    codeMask = (1 shl codeSize) - 1
                    available = clear + 2
                    oldCode = NULL_CODE
                    continue
                } else if (code == endOfInformation) {
                    break
                } else if (oldCode == NULL_CODE) {
                    dstPixels[pi] = suffix[code]
                    ++pi
                    ++i
                    oldCode = code
                    first = code
                    continue
                }
                inCode = code
                if (code >= available) {
                    pixelStack[top] = first.toByte()
                    ++top
                    code = oldCode
                }
                while (code >= clear) {
                    pixelStack[top] = suffix[code]
                    ++top
                    code = prefix[code].toInt()
                }
                first = suffix[code].toInt() and MASK_INT_LOWEST_BYTE
                dstPixels[pi] = first.toByte()
                ++pi
                ++i
                while (top > 0) {
                    // Pop a pixel off the pixel stack.
                    dstPixels[pi] = pixelStack[--top]
                    ++pi
                    ++i
                }

                // Add a new string to the string table.
                if (available < MAX_STACK_SIZE) {
                    prefix[available] = oldCode.toShort()
                    suffix[available] = first.toByte()
                    ++available
                    if (available and codeMask == 0 && available < MAX_STACK_SIZE) {
                        ++codeSize
                        codeMask += available
                    }
                }
                oldCode = inCode
            }
        }

        // Clear missing pixels.
        dstPixels.fill(COLOR_TRANSPARENT_BLACK.toByte(), pi, npix)

        return ImageDescriptor(
            offset = offset,
            dimension = dimension,
            localColorMap = localMap,
            pixels = dstPixels,
            interlaced = interlaced
        )
    }

    fun BufferedSource.readApplicationId(): String {
        skip(1)
        return readString(11, Charsets.US_ASCII)
    }

    fun BufferedSource.readLoopCount(): Short {
        skip(2)
        val count = readShortLe()
        skip(1)

        return count
    }

    fun BufferedSource.skipExtensionBlock() {
        var subBlockSize: Long = readByte().toLong()
        while (subBlockSize != 0L) {
            skip(subBlockSize)
            subBlockSize = readByte().toLong()
        }
    }

    @Throws(UnsupportedOperationException::class)
    fun BufferedSource.readGraphicControl(): GraphicControl {
        val blockSize = readByte()
        if (blockSize != 4.toByte()) throw UnsupportedOperationException("Block size of the graphic control should be 4")

        val packedField = readByte().toInt()
        val disposalMethod = packedField.shr(2) and 0b0111
        val hasTransparency = packedField and 0b0001 == 1

        val delayTime = readShortLe()
        val transparentColorIndex = readByte()

        val terminator = readByte()
        if (terminator != 0.toByte()) throw UnsupportedOperationException("Terminator not properly set")
        if (disposalMethod >= GraphicControl.Disposal.values().size) {
            throw java.lang.UnsupportedOperationException("Unsupported disposal method")
        }

        return GraphicControl(
            disposal = GraphicControl.Disposal.values()[disposalMethod],
            delayTime = delayTime,
            transparentColorIndex = if (hasTransparency) transparentColorIndex else null
        )
    }

    private fun BufferedSource.readBlock(block: ByteArray): Int {
        val blockSize = readByte().toUByte().toInt()
        if (blockSize > 0) {
            return read(block, 0, blockSize)
        }
        return 0
    }
}
