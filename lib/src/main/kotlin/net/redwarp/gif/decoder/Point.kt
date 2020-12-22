package net.redwarp.gif.decoder

data class Point(val x: Int, val y: Int) {
    constructor(x: Short, y: Short) : this(x.toInt(), y.toInt())
}
