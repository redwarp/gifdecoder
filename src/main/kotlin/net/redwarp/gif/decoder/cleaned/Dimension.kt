package net.redwarp.gif.decoder.cleaned
data class Dimension(val width: Int, val height: Int) {
    constructor(width: Short, height: Short) : this(width.toInt(), height.toInt())

    val size = width * height
}
