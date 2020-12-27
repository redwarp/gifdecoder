package net.redwarp.gif.decoder

import net.redwarp.gif.decoder.descriptors.Dimension
import net.redwarp.gif.decoder.descriptors.GifDescriptor
import net.redwarp.gif.decoder.descriptors.GraphicControlExtension
import net.redwarp.gif.decoder.descriptors.ImageDescriptor
import net.redwarp.gif.decoder.descriptors.LogicalScreenDescriptor
import net.redwarp.gif.decoder.lzw.NativeLzwDecoder
import net.redwarp.gif.decoder.utils.Palettes
import java.io.File
import java.io.InputStream

private const val TRANSPARENT_COLOR = 0x0

class NativeGif(
    private val gifDescriptor: GifDescriptor,
) {
    constructor(inputStream: InputStream, pixelPacking: PixelPacking = PixelPacking.ARGB) :
        this(Parser.parse(inputStream, pixelPacking))

    constructor(
        file: File,
        pixelPacking: PixelPacking = PixelPacking.ARGB
    ) : this(Parser.parse(file, pixelPacking))

    private var lastRenderedFrame: Int = -1
    private var frameIndex = 0
    private val framePixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size).apply {
        // Fill the frame with the background color, unless that is transparent, because a new int array
        // is already initialized to zero.
        if (backgroundColor != TRANSPARENT_COLOR) fill(backgroundColor)
    }
    private val scratch = ByteArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private var previousPixels: IntArray? = null

    private val lzwDecoder: NativeLzwDecoder = NativeLzwDecoder()

    private val isTransparent: Boolean =
        gifDescriptor.imageDescriptors.any { it.graphicControlExtension?.transparentColorIndex != null }

    val currentIndex: Int get() = frameIndex

    /**
     * Returns the delay time of the current frame, in millisecond.
     * This delay represents how long we should show this frame before displaying the next one in the animation.
     * If the gif is not animated, returns zero.
     * Some animated GIFs have a specified delay of 0L, meaning we should draw the next frame as fast as possible.
     */
    val currentDelay: Long
        get() {
            return if (!isAnimated) {
                0L
            } else {
                val delay =
                    gifDescriptor.imageDescriptors[frameIndex].graphicControlExtension?.delayTime?.let {
                        it.toLong() * 10L
                    }

                delay ?: 0L
            }
        }

    val dimension: Dimension = gifDescriptor.logicalScreenDescriptor.dimension

    val frameCount: Int = gifDescriptor.imageDescriptors.size

    val loopCount: LoopCount = when (val count = gifDescriptor.loopCount) {
        null -> LoopCount.NoLoop
        0 -> LoopCount.Infinite
        else -> LoopCount.Fixed(count)
    }

    val aspectRatio: Double = run {
        val ratio = gifDescriptor.logicalScreenDescriptor.pixelAspectRatio.toInt() and 0xff
        if (ratio == 0) 1.0 else {
            (ratio + 15).toDouble() / 64.0
        }
    }

    val backgroundColor: Int =
        run {
            // If at last one of the frame is transparent, let's use transparent as the background color.
            if (isTransparent) {
                TRANSPARENT_COLOR
            } else {
                // First, look for the background color in the global color table if it exists. Default to transparent.
                gifDescriptor.logicalScreenDescriptor.backgroundColorIndex?.let {
                    gifDescriptor.globalColorTable?.get(it.toInt() and 0xff)
                } ?: TRANSPARENT_COLOR
            }
        }

    val isAnimated: Boolean = gifDescriptor.imageDescriptors.size > 1

    /**
     * Advance the frame index, and loop back to zero after the last frame.
     * Does not care about loop count.
     */
    fun advance() {
        if (isAnimated) {
            frameIndex = (currentIndex + 1) % frameCount
        }
    }

    fun getCurrentFrame(inPixels: IntArray) {
        if (lastRenderedFrame == frameIndex) {
            // We are redrawing a previously managed frame
            framePixels.copyInto(inPixels)
        } else {
            getFrame(frameIndex, inPixels)
            lastRenderedFrame = frameIndex
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
            imageDescriptor.localColorTable ?: gifDescriptor.globalColorTable
            ?: Palettes.createFakeColorMap(
                gifDescriptor.logicalScreenDescriptor.colorCount
            )

        val graphicControlExtension = imageDescriptor.graphicControlExtension

        val disposal = graphicControlExtension?.disposalMethod
            ?: GraphicControlExtension.Disposal.NOT_SPECIFIED

        if (disposal == GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS) {
            previousPixels = framePixels.clone()
        }

        // lzwDecoder.decode(
        //     imageData = imageDescriptor.imageData,
        //     scratch,
        //     imageDescriptor.dimension.size
        // )

        // fillPixels(
        //     framePixels,
        //     scratch,
        //     colorTable,
        //     gifDescriptor.logicalScreenDescriptor,
        //     imageDescriptor
        // )
        val transparentColorIndexRef: Byte? =
            imageDescriptor.graphicControlExtension?.transparentColorIndex
        val transparentColorIndex: Int = if (transparentColorIndexRef != null) {
            transparentColorIndexRef.toInt() and 0xFF
        } else {
            1024
        }

        // lzwDecoder.fillPixels(
        //     pixels = framePixels,
        //     colorData = scratch,
        //     colorTable = colorTable,
        //     transparentColorIndex = transparentColorIndex,
        //     imageWidth = gifDescriptor.logicalScreenDescriptor.dimension.width,
        //     frameWidth = imageDescriptor.dimension.width,
        //     frameHeight = imageDescriptor.dimension.height,
        //     offsetX = imageDescriptor.position.x,
        //     offsetY = imageDescriptor.position.y,
        //     interlaced = imageDescriptor.isInterlaced
        // )

        lzwDecoder.decodeFull(
            imageData = imageDescriptor.imageData,
            scratch = scratch,
            pixels = framePixels, colorTable = colorTable,
            transparentColorIndex = transparentColorIndex,
            imageWidth = gifDescriptor.logicalScreenDescriptor.dimension.width,
            frameWidth = imageDescriptor.dimension.width,
            frameHeight = imageDescriptor.dimension.height,
            offsetX = imageDescriptor.position.x,
            offsetY = imageDescriptor.position.y,
            interlaced = imageDescriptor.isInterlaced
        )

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
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        if (imageDescriptor.isInterlaced) {
            fillPixelsInterlaced(
                pixels,
                colorData,
                colorTable,
                logicalScreenDescriptor,
                imageDescriptor
            )
        } else {
            fillPixelsSimple(
                pixels,
                colorData,
                colorTable,
                logicalScreenDescriptor,
                imageDescriptor
            )
        }
    }

    private fun fillPixelsSimple(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        val transparentColorIndex = imageDescriptor.graphicControlExtension?.transparentColorIndex
        val frameWidth = imageDescriptor.dimension.width
        val (offset_x, offset_y) = imageDescriptor.position
        val imageWidth = logicalScreenDescriptor.dimension.width

        for (index in 0 until imageDescriptor.dimension.size) {
            val colorIndex = colorData[index]
            if (colorIndex != transparentColorIndex) {
                val color = colorTable[colorIndex.toInt() and 0xff]
                val x = index % frameWidth
                val y = index / frameWidth
                val pixelIndex =
                    (y + offset_y) * imageWidth + offset_x + x
                pixels[pixelIndex] = color
            }
        }
    }

    private fun fillPixelsInterlaced(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor
    ) {
        val transparentColorIndex = imageDescriptor.graphicControlExtension?.transparentColorIndex
        val imageWidth = logicalScreenDescriptor.dimension.width
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
                        val color = colorTable[colorIndex.toInt() and 0xff]

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
}
