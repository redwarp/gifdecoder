package net.redwarp.gif.decoder.cleaned

class GifDescriptor(
    val header: Header,
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: ColorTable?,
    val loopCount: Int?,
    val imageDescriptors: List<ImageDescriptor>
)
