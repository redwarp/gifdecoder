package net.redwarp.gif.decoder

import java.lang.System.arraycopy

class ImageDescriptor(
    val offset: Point,
    val dimension: Dimension,
    val localColorMap: GifParser.ColorTable?,
    val pixels: ByteArray,
    val interlaced: Boolean
) {
    /**
     * @param globalColorMap To be used if the local map is null
     */
    fun fillPixels(destinationPixels: IntArray, destinationDimension: Dimension, globalColorMap: GifParser.ColorTable) {
        val map = localColorMap ?: globalColorMap

        var lineOffset = 0
        var pass = 0
        if (interlaced) {
            fillPixelsInterlaced(destinationPixels, destinationDimension, globalColorMap)
        } else {
            pixels.forEachIndexed { index, colorIndex ->
                val x = index % dimension.width
                val y = index / dimension.width

                val destinationIndex = (y + offset.y) * destinationDimension.width + x + offset.x
                destinationPixels[destinationIndex] = map[colorIndex]
            }
        }
    }

    private fun fillPixelsInterlaced(
        destinationPixels: IntArray,
        destinationDimension: Dimension,
        globalColorMap: GifParser.ColorTable
    ) {
        val w = dimension.width
        val h = dimension.height
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

        val flatPixels = ByteArray(pixels.size)

        while (from < set2) {
            arraycopy(pixels, from, flatPixels, to, w)
            from += w
            to += w8
        }
        run {
            to = w4
            while (from < set3) {
                arraycopy(pixels, from, flatPixels, to, w)
                from += w
                to += w8
            }
        }
        run {
            to = w2
            while (from < set4) {
                arraycopy(pixels, from, flatPixels, to, w)
                from += w
                to += w4
            }
        }
        run {
            to = w
            while (from < wh) {
                arraycopy(pixels, from, flatPixels, to, w)
                from += w
                to += w2
            }
        }

        val map = localColorMap ?: globalColorMap
        flatPixels.forEachIndexed { index, colorIndex ->
            val x = index % dimension.width
            val y = index / dimension.width

            val destinationIndex = (y + offset.y) * destinationDimension.width + x + offset.x
            destinationPixels[destinationIndex] = map[colorIndex]
        }
    }
}

data class GraphicControl(
    val disposal: Disposal,
    val delayTime: Short,
    val transparentColorIndex: Byte?
) {

    enum class Disposal {
        NOT_SPECIFIED, DO_NOT_DISPOSE, RESTORE_TO_BACKGROUND, RESTORE_TO_PREVIOUS
    }
}

data class Point(val x: Int, val y: Int) {
    constructor(x: Short, y: Short) : this(x.toInt(), y.toInt())
}

data class Dimension(val width: Int, val height: Int) {
    constructor(width: Short, height: Short) : this(width.toInt(), height.toInt())

    val size = width * height
}
