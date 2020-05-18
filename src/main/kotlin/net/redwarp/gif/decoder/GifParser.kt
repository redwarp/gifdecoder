package net.redwarp.gif.decoder

import okio.*
import java.io.File
import java.io.InputStream
import java.lang.UnsupportedOperationException

@ExperimentalUnsignedTypes
class GifParser(gifSource: Source) {
    constructor(file: File) : this(file.source())
    constructor(inputStream: InputStream) : this(inputStream.source())

    private val source: BufferedSource = gifSource.buffer()
    val header: Header = source.readHeader()
    val width: Short = source.readShortLe()
    val height: Short = source.readShortLe()
    val globalColorMap: ColorMap = source.readColorMap()

    init {
    }

    private fun BufferedSource.readHeader(): Header {
        return when (val header = source.readByteString(6L).string(Charsets.US_ASCII)) {
            "GIF87a" -> Header.GIF87a
            "GIF89a" -> Header.GIF89a
            else -> throw UnsupportedOperationException("Unsupported header type: $header")
        }
    }


    private fun BufferedSource.readColorMap(): ColorMap {
        val mask: UByte = 0b1000_0000u
        val colorInfo: UByte = source.readByte().toUByte()
        val global: UByte = (mask and colorInfo).toUInt().shr(7).toUByte()

        println("Global: $global")

        val pixelMask: UByte = 0b0000_0111u
        val pixelCount = (pixelMask and colorInfo).toInt()

        val size = 1.shl(pixelCount + 1)

        val backgroundIndex = source.readByte().toUByte().toInt()
        source.readByte()

        val colors = IntArray(size)
        for (colorIndex in 0 until size) {
            val r = source.readByte().toUInt()
            val g = source.readByte().toUInt()
            val b = source.readByte().toUInt()

            val color: UInt = 0xff000000u or r.shl(16) or g.shl(8) or b
            colors[colorIndex] = color.toInt()
        }

        return ColorMap(size, backgroundIndex, colors)
    }

    enum class Header {
        GIF87a, GIF89a
    }

    class ColorMap(val size: Int, val backgroundIndex: Int, val colors: IntArray) {

    }
}