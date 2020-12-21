package net.redwarp.gif.decoder

import okio.BufferedSource
import okio.Source
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream

private const val IMAGE_DESCRIPTOR_SEPARATOR = 0x2c.toByte()
private const val GIF_TERMINATOR = 0x3b.toByte()
private const val EXTENSION_INTRODUCER = 0x21.toByte()
private const val APPLICATION_EXTENSION = 0xff.toByte()
private const val GRAPHIC_CONTROL_EXTENSION = 0xf9.toByte()

class GifParser(gifSource: Source) : GifBuffer {
    constructor(file: File) : this(file.source())
    constructor(inputStream: InputStream) : this(inputStream.source())

    private val source: BufferedSource = gifSource.buffer()
    private var pendingGraphicControl: GraphicControl? = null

    val header: Header = source.readHeader()
    val width: Int = source.readShortLe().toInt()
    val height: Int = source.readShortLe().toInt()

    private val globalColorTablePackedField = source.readByte().toInt()
    val backgroundColorIndex = source.readByte().toInt()

    val globalColorMap: ColorTable = source.readGlobalColorMap(globalColorTablePackedField)

    var imageDescriptors: MutableList<ImageDescriptor> = mutableListOf()
    var loopCount: Short = 0

    fun parseContent() {
        var allDone = false
        do {
            val instruction = source.readByte()
            when (instruction) {
                IMAGE_DESCRIPTOR_SEPARATOR -> {
                    imageDescriptors.add(source.readImageDescriptor(pendingGraphicControl))
                    pendingGraphicControl = null // One graphicControl per frame
                }
                GIF_TERMINATOR -> {
                    allDone = true
                }
                EXTENSION_INTRODUCER -> {
//                    if (header == Header.GIF87a) throw UnsupportedOperationException("GIF87a do not support extensions")
                    parseExtension()
                }
            }
        } while (!allDone)
    }

    private fun parseExtension() {
        when (source.readByte()) {
            APPLICATION_EXTENSION -> {
                val applicationId = source.readApplicationId()
                if (applicationId == "NETSCAPE2.0" || applicationId == "ANIMEXTS1.0") {
                    loopCount = source.readLoopCount()
                } else {
                    // There might be other extensions out there but... we probably don't care.
                    source.skipExtensionBlock()
                }
            }
            GRAPHIC_CONTROL_EXTENSION -> {
                pendingGraphicControl = source.readGraphicControl()
            }
        }
    }

    enum class Header {
        GIF87a, GIF89a
    }

    class ColorTable(val size: Int, val colors: IntArray) {
        operator fun get(index: Byte): Int {
            return colors[index.toInt()]
        }
    }
}
