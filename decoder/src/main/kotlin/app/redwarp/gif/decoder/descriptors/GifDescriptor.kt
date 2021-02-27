package app.redwarp.gif.decoder.descriptors

import app.redwarp.gif.decoder.streams.ReplayInputStream

class GifDescriptor(
    val header: Header,
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: IntArray?,
    val loopCount: Int?,
    val imageDescriptors: List<ImageDescriptor>,
    val data: ReplayInputStream
)
