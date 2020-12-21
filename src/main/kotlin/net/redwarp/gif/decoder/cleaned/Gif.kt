package net.redwarp.gif.decoder.cleaned

class Gif(private val gifDescriptor: GifDescriptor) {
    private var frameIndex = 0
    private val framePixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)
    private val workPixels = IntArray(gifDescriptor.logicalScreenDescriptor.dimension.size)

    val currentIndex: Int get() = frameIndex

    fun getCurrentFrame(): IntArray {
        TODO()
    }

    fun getFrame(index: Int) {
        val imageDescriptor = gifDescriptor.imageDescriptors[index]
        val colorTable = imageDescriptor.localColorTable ?: gifDescriptor.globalColorTable

        val graphicControlExtension = imageDescriptor.graphicControlExtension

        val backgroundColor = colorTable.colors[gifDescriptor.logicalScreenDescriptor.backgroundColorIndex]

        val lzwDecoder = LzwDecoder(imageData = imageDescriptor.imageData)

        when (graphicControlExtension?.disposalMethod ?: GraphicControlExtension.Disposal.NOT_SPECIFIED) {
            GraphicControlExtension.Disposal.NOT_SPECIFIED, GraphicControlExtension.Disposal.DO_NOT_DISPOSE -> {
                framePixels.fill(backgroundColor)

                lzwDecoder.decode(workPixels, colorTable, graphicControlExtension?.transparentColorIndex)
            }
        }
    }
}