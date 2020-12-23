package net.redwarp.gif.decoder

class GifDescriptor(
    val header: Header,
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: IntArray?,
    val loopCount: Int?,
    val imageDescriptors: List<ImageDescriptor>
)
