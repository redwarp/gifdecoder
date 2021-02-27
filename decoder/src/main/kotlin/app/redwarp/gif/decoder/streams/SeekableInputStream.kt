package net.redwarp.gif.decoder.streams

import java.io.InputStream

abstract class SeekableInputStream : InputStream() {
    abstract fun seek(position: Int)

    abstract fun getPosition(): Int
}