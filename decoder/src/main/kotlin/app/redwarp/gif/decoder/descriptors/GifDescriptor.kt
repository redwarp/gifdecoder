package app.redwarp.gif.decoder.descriptors

class GifDescriptor(
    val header: Header,
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: IntArray?,
    val loopCount: Int?,
    val imageDescriptors: List<ImageDescriptor>
)
