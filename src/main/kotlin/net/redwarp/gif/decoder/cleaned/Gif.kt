package net.redwarp.gif.decoder.cleaned

class Gif(private val gifDescriptor: GifDescriptor) {
    private var frameIndex = 0
    private val framePixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private val scratch = ByteArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private var previousPixels: IntArray? = null

    val currentIndex: Int get() = frameIndex

    fun getCurrentFrame(): IntArray {
        TODO()
    }

    fun getFrame(index: Int): IntArray {
        val pixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
        getFrame(index, pixels)
        return pixels
    }

    fun getFrame(index: Int, inPixels: IntArray) {
        val imageDescriptor = gifDescriptor.imageDescriptors[index]
        val colorTable = imageDescriptor.localColorTable ?: gifDescriptor.globalColorTable

        val graphicControlExtension = imageDescriptor.graphicControlExtension

        val lzwDecoder = LzwDecoder(imageData = imageDescriptor.imageData)

        val disposal = graphicControlExtension?.disposalMethod ?: GraphicControlExtension.Disposal.NOT_SPECIFIED

        if (disposal == GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS) {
            previousPixels = framePixels.clone()
        }

        lzwDecoder.decode(scratch)
        fillPixels(framePixels, scratch, colorTable, gifDescriptor.logicalScreenDescriptor, imageDescriptor)

        framePixels.copyInto(inPixels)
    }

    fun fillPixels(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: ColorTable,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        if (imageDescriptor.isInterlaced) {
            fillPixelsInterlaced(pixels, colorData, colorTable, logicalScreenDescriptor, imageDescriptor)
        } else {
            fillPixelsSimple(pixels, colorData, colorTable, logicalScreenDescriptor, imageDescriptor)
        }
    }

    private fun fillPixelsSimple(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: ColorTable,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        for (index in 0 until imageDescriptor.dimension.size) {
            val colorCode = colorData[index]
            if (colorCode != logicalScreenDescriptor.backgroundColorIndex) {
                val color = colorTable.colors[colorCode.toInt()]
                val x = index % imageDescriptor.dimension.width
                val y = index % imageDescriptor.dimension.height
                val pixelIndex =
                    (y + imageDescriptor.position.y) * logicalScreenDescriptor.dimension.width + imageDescriptor.position.x + x
                pixels[pixelIndex] = color
            }
        }
    }

    private fun fillPixelsInterlaced(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: ColorTable,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        val w = imageDescriptor.dimension.width
        val h = imageDescriptor.dimension.height
        val wh = w * h

        // Interlaced images are organized in 4 sets of pixel lines
        val set2Y: Int = h + 7 ushr 3 // Line no. = ceil(h/8.0)
        val set3Y: Int = set2Y + (h + 3 ushr 3) // ceil(h-4/8.0)
        val set4Y: Int = set3Y + (h + 1 ushr 2) // ceil(h-2/4.0)

        // Sets' start indices in source array
        val set2: Int = w * set2Y
        val set3: Int = w * set3Y
        val set4: Int = w * set4Y
        // Line skips in destination array
        val w2: Int = w shl 1
        val w4 = w2 shl 1
        val w8 = w4 shl 1
        // Group 1 contains every 8th line starting from 0
        var from = 0
        var to = 0

        val flatPixels = ByteArray(imageDescriptor.dimension.size)

        while (from < set2) {
            System.arraycopy(colorData, from, flatPixels, to, w)
            from += w
            to += w8
        }
        run {
            to = w4
            while (from < set3) {
                System.arraycopy(colorData, from, flatPixels, to, w)
                from += w
                to += w8
            }
        }
        run {
            to = w2
            while (from < set4) {
                System.arraycopy(colorData, from, flatPixels, to, w)
                from += w
                to += w4
            }
        }
        run {
            to = w
            while (from < wh) {
                System.arraycopy(colorData, from, flatPixels, to, w)
                from += w
                to += w2
            }
        }

        fillPixelsSimple(pixels, flatPixels, colorTable, logicalScreenDescriptor, imageDescriptor)
    }
}