package net.redwarp.gif.decoder

class ImageDescriptor(
    val position: Point,
    val dimension: Dimension,
    val isInterlaced: Boolean,
    val localColorTable: IntArray?,
    val imageData: ByteArray,
    val graphicControlExtension: GraphicControlExtension?
)
