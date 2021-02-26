package app.redwarp.gif.decoder.descriptors

data class GraphicControlExtension(
    val disposalMethod: Disposal,
    val delayTime: UShort,
    val transparentColorIndex: Byte?
) {
    enum class Disposal {
        NOT_SPECIFIED,
        DO_NOT_DISPOSE, // Keep the previous frame in place.
        RESTORE_TO_BACKGROUND, // After the image is shown, restore the whole canvas to the background color.
        RESTORE_TO_PREVIOUS // After these pixels are shown, restore the canvas to the previous image.
    }
}
