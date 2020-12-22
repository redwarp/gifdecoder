package net.redwarp.gif.decoder.cleaned

data class LogicalScreenDescriptor(
    val dimension: Dimension,
    val hasGlobalColorTable: Boolean,
    val sizeOfGlobalColorTable: Int,
    val backgroundColorIndex: Byte?,
    val pixelAspectRatio: Byte
) {

    val colorCount: Int
        get() {
            return 1.shl(sizeOfGlobalColorTable + 1)
        }
}
