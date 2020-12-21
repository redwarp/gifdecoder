package net.redwarp.gif.decoder.cleaned

data class LogicalScreenDescriptor(
    val dimension: Dimension,
    val packedFields: UByte,
    val backgroundColorIndex: Byte,
    val pixelAspectRatio: Byte
) {
    val hasGlobalColorTable: Boolean
        get() {
            val mask: UByte = 0b1000_0000u
            return (mask and packedFields) == mask
        }

    val sizeOfGlobalColorTable: Int
        get() {
            val mask: UByte = 0b0000_0111u
            return (mask and packedFields).toInt()
        }
}
