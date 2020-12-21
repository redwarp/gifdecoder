package net.redwarp.gif.decoder.cleaned

data class GraphicControlExtension(
    val disposalMethod: Disposal,
    val delayTime: Short,
    val transparentColorIndex: Byte?
) {
    enum class Disposal {
        NOT_SPECIFIED, DO_NOT_DISPOSE, RESTORE_TO_BACKGROUND, RESTORE_TO_PREVIOUS
    }
}
