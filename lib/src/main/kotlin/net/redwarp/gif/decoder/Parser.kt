package net.redwarp.gif.decoder

import okio.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private const val IMAGE_DESCRIPTOR_SEPARATOR = 0x2c.toByte()
private const val GIF_TERMINATOR = 0x3b.toByte()
private const val EXTENSION_INTRODUCER = 0x21.toByte()
private const val APPLICATION_EXTENSION = 0xff.toByte()
private const val GRAPHIC_CONTROL_EXTENSION = 0xf9.toByte()
private const val NETSCAPE = "NETSCAPE2.0"
private const val ANIMEXTS = "ANIMEXTS1.0"

/**
 * Based on https://web.archive.org/web/20160304075538/http://qalle.net/gif89a.php
 */
class Parser {

    fun parse(file: File): GifDescriptor = parse(file.source())

    fun parse(inputStream: InputStream): GifDescriptor = parse(inputStream.source())

    private fun parse(source: Source): GifDescriptor {
        source.buffer().use { bufferedSource ->
            val header = bufferedSource.parseHeader()
            val logicalScreenDescriptor = bufferedSource.parseLogicalScreenDescriptor()

            val globalColorTable: ColorTable? = if (logicalScreenDescriptor.hasGlobalColorTable) {
                bufferedSource.parseColorTable(logicalScreenDescriptor.colorCount)
            } else {
                null
            }

            val (loopCount, imageDescriptors) = parseLoop(bufferedSource)

            return GifDescriptor(header, logicalScreenDescriptor, globalColorTable, loopCount, imageDescriptors)
        }
    }

    @Throws(InvalidGifException::class)
    private fun BufferedSource.parseHeader(): Header {
        return when (val headerString = readByteString(6L).string(Charsets.US_ASCII)) {
            "GIF87a" -> Header.GIF87a
            "GIF89a" -> Header.GIF89a
            else -> throw InvalidGifException("$headerString is not a valid GIF header")
        }
    }

    private fun BufferedSource.parseLogicalScreenDescriptor(): LogicalScreenDescriptor {
        val dimension = Dimension(readShortLe(), readShortLe())
        val packedFields = readByte().toUByte()
        val hasGlobalColorTableMask: UByte = 0b1000_0000u
        val newValue = packedFields xor hasGlobalColorTableMask
        val hasGlobalColorTable = (hasGlobalColorTableMask and packedFields) == hasGlobalColorTableMask
        val sizeOfGlobalColorTableMask: UByte = 0b0000_0111u
        val sizeOfGlobalColorTable = (sizeOfGlobalColorTableMask and packedFields).toInt()

        val backgroundColorIndex = readByte()

        return LogicalScreenDescriptor(
            dimension = dimension,
            hasGlobalColorTable = hasGlobalColorTable,
            sizeOfGlobalColorTable = sizeOfGlobalColorTable,
            // If there is no global color table, the background color index is meaningless.
            backgroundColorIndex = if (hasGlobalColorTable) backgroundColorIndex else null,
            pixelAspectRatio = readByte()
        )
    }

    private fun BufferedSource.parseColorTable(colorCount: Int): ColorTable {
        val colors = IntArray(colorCount)
        for (colorIndex in 0 until colorCount) {
            val r = readByte().toInt()
            val g = readByte().toInt()
            val b = readByte().toInt()

            val color: Int =
                0xff000000.toInt() or (r.shl(16) and 0x00ff0000) or (g.shl(8) and 0x0000ff00) or (b and 0x000000ff)
            colors[colorIndex] = color
        }

        return ColorTable(colors)
    }

    private fun BufferedSource.parseGraphicControl(): GraphicControlExtension {
        val blockSize = readByte()
        if (blockSize != 4.toByte()) throw InvalidGifException("Block size of the graphic control should be 4")

        val packedField = readByte().toInt()
        val disposalMethod = packedField.shr(2) and 0b0111
        val hasTransparency = packedField and 0b0001 == 1

        val delayTime = readShortLe().toUShort()
        val transparentColorIndex = readByte()

        val terminator = readByte()
        if (terminator != 0.toByte()) throw InvalidGifException("Terminator not properly set")
        if (disposalMethod >= GraphicControlExtension.Disposal.values().size) {
            throw InvalidGifException("Unsupported disposal method")
        }

        return GraphicControlExtension(
            disposalMethod = GraphicControlExtension.Disposal.values()[disposalMethod],
            delayTime = delayTime,
            transparentColorIndex = if (hasTransparency) transparentColorIndex else null
        )
    }

    private fun BufferedSource.parseApplicationId(): String {
        skip(1)
        return readString(11, Charsets.US_ASCII)
    }

    private fun BufferedSource.parseLoopCount(): Int {
        skip(2)
        val count = readShortLe().toInt()
        skip(1)

        return count
    }

    private fun BufferedSource.skipSubBlocks() {
        var subBlockSize: Long = readByte().toLong()
        while (subBlockSize != 0L) {
            skip(subBlockSize)
            subBlockSize = readByte().toLong()
        }
    }

    private fun parseLoop(bufferedSource: BufferedSource): Pair<Int?, List<ImageDescriptor>> {
        var loopCount: Int? = 0
        var pendingGraphicControl: GraphicControlExtension? = null
        val imageDescriptors: MutableList<ImageDescriptor> = mutableListOf()
        while (true) {
            when (bufferedSource.readByte()) {
                IMAGE_DESCRIPTOR_SEPARATOR -> {
                    imageDescriptors.add(bufferedSource.parseImageDescriptor(pendingGraphicControl))
                    pendingGraphicControl = null
                }
                GIF_TERMINATOR -> {
                    break
                }
                EXTENSION_INTRODUCER -> {
                    when (bufferedSource.readByte()) {
                        APPLICATION_EXTENSION -> {
                            val applicationId = bufferedSource.parseApplicationId()
                            if (applicationId == NETSCAPE || applicationId == ANIMEXTS) {
                                loopCount = bufferedSource.parseLoopCount()
                            } else {
                                // There might be other application extensions out there but...
                                // we probably don't care.
                                bufferedSource.skipSubBlocks()
                            }
                        }
                        GRAPHIC_CONTROL_EXTENSION -> {
                            pendingGraphicControl = bufferedSource.parseGraphicControl()
                        }
                        else -> {
                            bufferedSource.skipSubBlocks()
                        }
                    }
                }
            }
        }

        return Pair(loopCount, imageDescriptors)
    }

    private fun BufferedSource.parseImageDescriptor(
        graphicControlExtension: GraphicControlExtension?
    ): ImageDescriptor {
        val position = Point(readShortLe(), readShortLe())
        val dimension = Dimension(readShortLe(), readShortLe())

        val packedFields = readByte().toUByte()

        val colorTableFlagMask: UByte = 0b1000_0000u
        val usesLocalColorTable = (packedFields and colorTableFlagMask) == colorTableFlagMask

        val interlacedMask: UByte = 0b0100_0000u
        val isInterlaced = (packedFields and interlacedMask) == interlacedMask

        val sizeOfLocalTableMask: UByte = 0b0000_0111u
        val sizeOfLocalTable = (sizeOfLocalTableMask and packedFields).toInt()
        val colorCount = 1.shl(sizeOfLocalTable + 1)
        val localColorTable: ColorTable? = if (usesLocalColorTable) {
            parseColorTable(colorCount)
        } else {
            null
        }

        val imageData = readImageData()

        return ImageDescriptor(
            position = position,
            dimension = dimension,
            isInterlaced = isInterlaced,
            localColorTable = localColorTable,
            imageData = imageData,
            graphicControlExtension = graphicControlExtension
        )
    }

    internal fun BufferedSource.readImageData(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val sink = byteArrayOutputStream.sink().buffer()

        // LZW Minimum Code Size
        sink.writeByte(readByte().toInt())

        while (true) {
            val blockSize = readByte().toUByte().toInt()
            sink.writeByte(blockSize)
            if (blockSize == 0) {
                break
            }
            sink.write(this, blockSize.toLong())
        }

        sink.flush()
        return byteArrayOutputStream.toByteArray()
    }
}
