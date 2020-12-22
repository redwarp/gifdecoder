package net.redwarp.gif.decoder.cleaned

private const val TRANSPARENT_COLOR = 0x0

class Gif(private val gifDescriptor: GifDescriptor) {
    private var frameIndex = 0
    private val framePixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size).apply {
        // Fill the frame with the background color, unless that is transparent, because a new int array
        // is already initialized to zero.
        if (backgroundColor != TRANSPARENT_COLOR) fill(backgroundColor)
    }
    private val scratch = ByteArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private var previousPixels: IntArray? = null

    val currentIndex: Int get() = frameIndex

    val dimension: Dimension = gifDescriptor.logicalScreenDescriptor.dimension

    val frameCount: Int = gifDescriptor.imageDescriptors.size

    val backgroundColor: Int =
        run {
            // If at last one of the frame is transparent, let's use transparent as the background color.
            if (gifDescriptor.imageDescriptors.any { it.graphicControlExtension?.transparentColorIndex != null }) {
                TRANSPARENT_COLOR
            } else {
                // First, look for the background color in the global color table if it exists. Default to transparent.
                gifDescriptor.logicalScreenDescriptor.backgroundColorIndex?.let {
                    gifDescriptor.globalColorTable?.colors?.get(it.toInt() and 0xff)
                } ?: TRANSPARENT_COLOR
            }
        }

    fun getFrame(index: Int): IntArray {
        val pixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
        getFrame(index, pixels)
        return pixels
    }

    fun getFrame(index: Int, inPixels: IntArray) {
        val imageDescriptor = gifDescriptor.imageDescriptors[index]
        val colorTable =
            imageDescriptor.localColorTable ?: gifDescriptor.globalColorTable ?: Palettes.createFakeColorMap(
                gifDescriptor.logicalScreenDescriptor.colorCount
            )

        val graphicControlExtension = imageDescriptor.graphicControlExtension

        val lzwDecoder = LzwDecoder(imageData = imageDescriptor.imageData)

        val disposal = graphicControlExtension?.disposalMethod ?: GraphicControlExtension.Disposal.NOT_SPECIFIED

        if (disposal == GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS) {
            previousPixels = framePixels.clone()
        }

        lzwDecoder.decode(scratch)
        fillPixels(framePixels, scratch, colorTable, gifDescriptor.logicalScreenDescriptor, imageDescriptor)

        framePixels.copyInto(inPixels)

        when (disposal) {
            GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS -> {
                previousPixels?.copyInto(framePixels)
            }
            GraphicControlExtension.Disposal.NOT_SPECIFIED -> Unit // Unspecified, we do nothing.
            GraphicControlExtension.Disposal.DO_NOT_DISPOSE -> Unit // Do not dispose, we do nothing.
            GraphicControlExtension.Disposal.RESTORE_TO_BACKGROUND -> {
                // Restore the section drawn for this frame to the background color.
                val (frame_width, frame_height) = imageDescriptor.dimension
                val (offset_x, offset_y) = imageDescriptor.position

                for (line in 0 until frame_height) {
                    val startIndex = (line + offset_y) * dimension.width + offset_x
                    framePixels.fill(backgroundColor, startIndex, startIndex + frame_width)
                }
            }
        }
    }

    private fun fillPixels(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: ColorTable,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        if (imageDescriptor.isInterlaced) {
            fillPixelsInterlaced2(pixels, colorData, colorTable, logicalScreenDescriptor, imageDescriptor)
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
        val transparentColorIndex = imageDescriptor.graphicControlExtension?.transparentColorIndex
        val frameWidth = imageDescriptor.dimension.width
        val (offset_x, offset_y) = imageDescriptor.position
        for (index in 0 until imageDescriptor.dimension.size) {
            val colorIndex = colorData[index]
            if (colorIndex != transparentColorIndex) {
                val color = colorTable.colors[colorIndex.toInt() and 0xff]
                val x = index % frameWidth
                val y = index / frameWidth
                val pixelIndex =
                    (y + offset_y) * logicalScreenDescriptor.dimension.width + offset_x + x
                pixels[pixelIndex] = color
            }
        }
    }

    private fun fillPixelsInterlaced2(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: ColorTable,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        val transparentColorIndex = imageDescriptor.graphicControlExtension?.transparentColorIndex
        val (imageWidth, _) = logicalScreenDescriptor.dimension
        val (frameWidth, frameHeight) = imageDescriptor.dimension
        val (offset_x, offset_y) = imageDescriptor.position
        var pass = 0
        var stride = 8
        var matchedLine = 0


        var lineIndex = 0
        while (pass < 4) {
            while (matchedLine < frameHeight) {
                val copyFromIndex = lineIndex * frameWidth
                val copyToIndex = (matchedLine + offset_y) * imageWidth + offset_x
                val indexOffset = copyToIndex - copyFromIndex

                for (index in copyFromIndex until copyFromIndex + frameWidth) {
                    val colorIndex = colorData[index]
                    if (colorIndex != transparentColorIndex) {
                        val color = colorTable.colors[colorIndex.toInt() and 0xff]

                        val pixelIndex = index + indexOffset
                        pixels[pixelIndex] = color
                    }
                }

                lineIndex++
                matchedLine += stride
            }

            pass++
            when (pass) {
                1 -> {
                    matchedLine = 4
                    stride = 8
                }
                2 -> {
                    matchedLine = 2
                    stride = 4
                }
                3 -> {
                    matchedLine = 1
                    stride = 2
                }
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
        val width = imageDescriptor.dimension.width
        val h = imageDescriptor.dimension.height
        val wh = width * h

        // Interlaced images are organized in 4 sets of pixel lines
        val set2Y: Int = h + 7 ushr 3 // Line no. = ceil(h/8.0)
        val set3Y: Int = set2Y + (h + 3 ushr 3) // ceil(h-4/8.0)
        val set4Y: Int = set3Y + (h + 1 ushr 2) // ceil(h-2/4.0)

        // Sets start indices in source array
        val set2: Int = width * set2Y
        val set3: Int = width * set3Y
        val set4: Int = width * set4Y
        // Line skips in destination array
        val w2: Int = width shl 1
        val w4 = w2 shl 1
        val w8 = w4 shl 1
        // Group 1 contains every 8th line starting from 0
        var from = 0
        var to = 0

        val flatPixels = ByteArray(imageDescriptor.dimension.size)

        while (from < set2) {
            System.arraycopy(colorData, from, flatPixels, to, width)
            from += width
            to += w8
        }
        run {
            to = w4
            while (from < set3) {
                System.arraycopy(colorData, from, flatPixels, to, width)
                from += width
                to += w8
            }
        }
        run {
            to = w2
            while (from < set4) {
                System.arraycopy(colorData, from, flatPixels, to, width)
                from += width
                to += w4
            }
        }
        run {
            to = width
            while (from < wh) {
                System.arraycopy(colorData, from, flatPixels, to, width)
                from += width
                to += w2
            }
        }

        fillPixelsSimple(pixels, flatPixels, colorTable, logicalScreenDescriptor, imageDescriptor)
    }
}