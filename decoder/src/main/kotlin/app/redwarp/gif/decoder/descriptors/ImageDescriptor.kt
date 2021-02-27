package app.redwarp.gif.decoder.descriptors

class ImageDescriptor(
    val position: Point,
    val dimension: Dimension,
    val isInterlaced: Boolean,
    val localColorTable: IntArray?,
    val imageData: ImageData,
    val graphicControlExtension: GraphicControlExtension?
)
