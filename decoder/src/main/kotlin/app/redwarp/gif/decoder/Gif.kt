/* Copyright 2020 Benoit Vermont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.redwarp.gif.decoder

import app.redwarp.gif.decoder.descriptors.Dimension
import app.redwarp.gif.decoder.descriptors.GifDescriptor
import app.redwarp.gif.decoder.descriptors.GraphicControlExtension
import app.redwarp.gif.decoder.descriptors.ImageDescriptor
import app.redwarp.gif.decoder.descriptors.LogicalScreenDescriptor
import app.redwarp.gif.decoder.descriptors.params.LoopCount
import app.redwarp.gif.decoder.descriptors.params.PixelPacking
import app.redwarp.gif.decoder.lzw.LzwDecoder
import app.redwarp.gif.decoder.utils.Palettes
import java.io.File
import java.io.InputStream

private const val TRANSPARENT_COLOR = 0x0

/**
 * Representation of the gif, with methods to decode frames.
 * This class's methods are not thread safe.
 */
class Gif(
    private val gifDescriptor: GifDescriptor,
) : AutoCloseable {
    private var frameIndex = 0
    private val framePixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private val scratch = ByteArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private val rawScratch = ByteArray(gifDescriptor.imageDescriptors.maxOf { it.imageData.length })

    private val lzwDecoder: LzwDecoder = LzwDecoder()

    private val isTransparent: Boolean =
        gifDescriptor.imageDescriptors.any { it.graphicControlExtension?.transparentColorIndex != null }

    private var previousRenderedFrame: Int = -1
    private val previousPixels: IntArray by lazy { IntArray(framePixels.size) }
    private var previousDisposal: GraphicControlExtension.Disposal? = null

    /**
     * Returns the index of the frame currently decoded in the frame buffer.
     */
    val currentIndex: Int get() = frameIndex

    /**
     * Returns the delay time of the current frame, in millisecond.
     * This delay represents how long we should show this frame before displaying the next one in the animation.
     * If the gif is not animated, returns zero.
     * Some animated GIFs have a specified delay of 0L, meaning we should draw the next frame as fast as possible.
     */
    val currentDelay: Long get() = getDelay(frameIndex)

    /**
     * The dimensions of the GIF, width and height.
     */
    val dimension: Dimension = gifDescriptor.logicalScreenDescriptor.dimension

    /**
     * How many frames in the GIF. If more than 1, we have an animated GIF.
     */
    val frameCount: Int = gifDescriptor.imageDescriptors.size

    /**
     * For animated gif, the loop count policy: should never loop, should loop forever
     * or should loop a set amount of time.
     */
    val loopCount: LoopCount = when (val count = gifDescriptor.loopCount) {
        null -> LoopCount.NoLoop
        0 -> LoopCount.Infinite
        else -> LoopCount.Fixed(count)
    }

    /**
     * The Pixel Aspect Ratio is defined to be the quotient of the pixel's
     * width over its height.  The value range in this field allows
     * specification of the widest pixel of 4:1 to the tallest pixel of 1:4
     */
    val aspectRatio: Double = run {
        val ratio = gifDescriptor.logicalScreenDescriptor.pixelAspectRatio.toInt() and 0xff
        if (ratio == 0) {
            1.0
        } else {
            (ratio + 15).toDouble() / 64.0
        }
    }

    /**
     * The background color as read from the global color table, default to transparent if not set.
     */
    val backgroundColor: Int =
        // If at last one of the frame is transparent, let's use transparent as the background color.
        if (isTransparent) {
            TRANSPARENT_COLOR
        } else {
            // First, look for the background color in the global color table if it exists. Default to transparent.
            gifDescriptor.logicalScreenDescriptor.backgroundColorIndex?.let {
                gifDescriptor.globalColorTable?.get(it.toInt() and 0xff)
            } ?: TRANSPARENT_COLOR
        }

    /**
     * A gif with more than 1 frame will be animated.
     */
    val isAnimated: Boolean = gifDescriptor.imageDescriptors.size > 1

    /**
     * Advance the frame index, decode the new frame, looping back to zero after the last frame
     * has been reached. Does not care about loop count.
     *
     * @return Success if the frame index was advanced and the matching frame properly decoded.
     */
    fun advance(): Result<Unit> {
        if (isAnimated) {
            if (previousRenderedFrame == -1) {
                // Frame 0 was never rendered, let's actually decode it first, as we need the
                // previous frame to compute the next.

                decodeFrame(0).onFailure {
                    return Result.failure(it)
                }
            }

            frameIndex = (currentIndex + 1) % frameCount

            return decodeFrame(frameIndex)
        }

        return Result.success(Unit)
    }

    /**
     * Write the current frame in the int array.
     *
     * @param inPixels The buffer where the pixels will be written.
     * @return Success(inPixels) if a frame was successfully written.
     */
    fun getCurrentFrame(inPixels: IntArray): Result<IntArray> {
        if (previousRenderedFrame == -1) {
            // Frame 0 was never rendered, let's actually decode it first, as we need the
            // previous frame to compute the next.
            decodeFrame(0).onFailure { return Result.failure(it) }
        }

        framePixels.copyInto(inPixels)
        return Result.success(inPixels)
    }

    /**
     * Get the frame at set index, returning a int array. It will internally advance the current
     * frame counter if needed, and draw each needed frame it turn, to make sure the result is
     * consistent.
     *
     * @param index The index of the frame to decode and return.
     * @return Success(pixels) containing the pixels.
     */
    fun getFrame(index: Int): Result<IntArray> {
        val pixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
        return getFrame(index, pixels)
    }

    /**
     * Get the frame at set index, writing it in the provided int array. It will internally advance
     * the current frame counter if needed, and draw each needed frame it turn, to make sure
     * the result is consistent.
     *
     * @param index The index of the frame to decode and return.
     * @param inPixels The buffer where the pixels will be written.
     * @return Success(inPixels) if a frame was successfully written.
     */
    fun getFrame(index: Int, inPixels: IntArray): Result<IntArray> {
        if (index !in 0 until frameCount) return Result.failure(IndexOutOfBoundsException("Index should be between 0 and ${frameCount - 1}, was $index"))

        while (currentIndex != index) {
            advance().onFailure { return Result.failure(it) }
        }
        getCurrentFrame(inPixels)
        return Result.success(inPixels)
    }

    /**
     * Returns the delay time of the specified frame, in millisecond.
     * This delay represents how long we should show this frame before displaying the next one in the animation.
     * If the gif is not animated, returns zero.
     * Some animated GIFs have a specified delay of 0L, meaning we should draw the next frame as fast as possible.
     * Most web browsers opt to draw a frame every 32 milliseconds if the delay is set to 0L.
     *
     * @param index The index of the frame we want the delay for.
     * @return The delay in milliseconds.
     */
    fun getDelay(index: Int): Long {
        return if (!isAnimated) {
            0L
        } else {
            gifDescriptor.imageDescriptors.getOrNull(index)?.graphicControlExtension?.delayTime?.let {
                it.toLong() * 10L
            } ?: 0L
        }
    }

    override fun close() {
        gifDescriptor.close()
    }

    /**
     * Decodes the frame at set index, making sure first to apply the previous frame
     * disposal policy. This method should only be called in sequence: frame 0, then 1, 2, ...
     * otherwise the disposal will produce unexpected results.
     *
     * @param index The index of the frame to decode. Does no check for out of bounds.
     * @return Success if the frame was properly decoded.
     */
    private fun decodeFrame(index: Int): Result<Unit> {
        // First, apply disposal of last frame.
        if (index == 0) {
            // Special case, we clear the canvas when we loop back to frame 0.
            framePixels.fill(backgroundColor)
            previousDisposal = null
        }

        when (previousDisposal) {
            GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS -> {
                previousPixels.copyInto(framePixels)
            }

            GraphicControlExtension.Disposal.NOT_SPECIFIED -> Unit // Unspecified, we do nothing.
            GraphicControlExtension.Disposal.DO_NOT_DISPOSE -> Unit // Do not dispose, we do nothing.
            GraphicControlExtension.Disposal.RESTORE_TO_BACKGROUND -> {
                // Restore the section drawn for this frame to the background color.
                val imageDescriptor = gifDescriptor.imageDescriptors[index.previousIndex]

                val (frameWidth, frameHeight) = imageDescriptor.dimension
                val (offsetX, offsetY) = imageDescriptor.position

                for (line in 0 until frameHeight) {
                    val startIndex = (line + offsetY) * dimension.width + offsetX
                    framePixels.fill(backgroundColor, startIndex, startIndex + frameWidth)
                }
            }

            null -> Unit
        }

        val imageDescriptor = gifDescriptor.imageDescriptors[index]
        val colorTable = imageDescriptor.localColorTable
            ?: gifDescriptor.globalColorTable
            ?: Palettes.createFakeColorMap(gifDescriptor.logicalScreenDescriptor.colorCount)

        val graphicControlExtension = imageDescriptor.graphicControlExtension

        // Prepare disposal of this frame
        val disposal = graphicControlExtension?.disposalMethod
            ?: GraphicControlExtension.Disposal.NOT_SPECIFIED

        if (disposal == GraphicControlExtension.Disposal.RESTORE_TO_PREVIOUS) {
            framePixels.copyInto(previousPixels)
        }
        previousDisposal = disposal

        return runCatching {
            // A gif descriptor might be shared between several gifs. So just in case, we make sure
            // that seeking and reading are grouped in a synchronized block to avoid artifacts or
            // crashes.
            synchronized(gifDescriptor) {
                gifDescriptor.data.seek(imageDescriptor.imageData.position)
                gifDescriptor.data.read(rawScratch, 0, imageDescriptor.imageData.length)
            }

            lzwDecoder.decode(imageData = rawScratch, scratch, framePixels.size)
            fillPixels(
                framePixels,
                scratch,
                colorTable,
                gifDescriptor.logicalScreenDescriptor,
                imageDescriptor,
            )

            previousRenderedFrame = index
        }
    }

    private fun fillPixels(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor,
    ) {
        if (imageDescriptor.isInterlaced) {
            fillPixelsInterlaced(
                pixels,
                colorData,
                colorTable,
                logicalScreenDescriptor,
                imageDescriptor,
            )
        } else {
            fillPixelsSimple(
                pixels,
                colorData,
                colorTable,
                logicalScreenDescriptor,
                imageDescriptor,
            )
        }
    }

    private fun fillPixelsSimple(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor,
    ) {
        val transparentColorIndex: Int = imageDescriptor.transparentColorIndex
        val frameWidth = imageDescriptor.dimension.width
        val (offsetX, offsetY) = imageDescriptor.position
        val imageWidth = logicalScreenDescriptor.dimension.width

        for (index in 0 until imageDescriptor.dimension.size) {
            val colorIndex = colorData[index].toInt() and 0xff
            if (colorIndex != transparentColorIndex) {
                val color = colorTable[colorIndex]
                val x = index % frameWidth
                val y = index / frameWidth
                val pixelIndex = (y + offsetY) * imageWidth + offsetX + x
                pixels[pixelIndex] = color
            }
        }
    }

    private fun fillPixelsInterlaced(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        logicalScreenDescriptor: LogicalScreenDescriptor,
        imageDescriptor: ImageDescriptor,
    ) {
        val transparentColorIndex = imageDescriptor.transparentColorIndex
        val imageWidth = logicalScreenDescriptor.dimension.width
        val (frameWidth, frameHeight) = imageDescriptor.dimension
        val (offsetX, offsetY) = imageDescriptor.position
        var pass = 0
        var stride = 8
        var matchedLine = 0

        var lineIndex = 0
        while (pass < 4) {
            while (matchedLine < frameHeight) {
                val copyFromIndex = lineIndex * frameWidth
                val copyToIndex = (matchedLine + offsetY) * imageWidth + offsetX
                val indexOffset = copyToIndex - copyFromIndex

                for (index in copyFromIndex until copyFromIndex + frameWidth) {
                    val colorIndex = colorData[index].toInt() and 0xff
                    if (colorIndex != transparentColorIndex) {
                        val color = colorTable[colorIndex]

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

    val Int.previousIndex get() = (this - 1 + frameCount) % frameCount

    companion object {
        /**
         * Open a gif file, using a [java.io.RandomAccessFile] to access the data: The gif wont be
         * loaded fully in memory. It allows handling crazy big files, as some gifs are actually
         * several hundred mega byte big, as some users seem to mistake them for movie files.
         *
         * @param file The file to open
         * @param pixelPacking Optional pixel packing, default to ARGB. It will affect the packing
         * of pixels when calling [Gif.decodeFrame] later.
         * @return Success if the gif headers where properly decoded.
         */
        fun from(
            file: File,
            pixelPacking: PixelPacking = PixelPacking.ARGB,
        ): Result<Gif> = Parser.parse(file, pixelPacking).map(::Gif)

        /**
         * Load a gif from an InputStream: The [InputStream] will be fully read and loaded in memory.
         *
         * @param inputStream The input stream containing the gif data.
         * @param pixelPacking Optional pixel packing, default to ARGB. It will affect the packing
         * of pixels when calling [Gif.decodeFrame] later.
         * @return Success if the gif headers where properly decoded.
         */
        fun from(
            inputStream: InputStream,
            pixelPacking: PixelPacking = PixelPacking.ARGB,
        ): Result<Gif> = Parser.parse(inputStream, pixelPacking).map(::Gif)

        /**
         * Creates a new gif from an existing gif.
         *
         * @return The new [Gif]
         */
        fun from(gif: Gif): Gif = Gif(gif.gifDescriptor.shallowClone())
    }
}
