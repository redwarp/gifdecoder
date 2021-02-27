package app.redwarp.gif.decoder.streams

import java.io.InputStream

/**
 * An [InputStream] that can be "replayed" by seeking a position.
 */
abstract class ReplayInputStream : InputStream() {
    abstract fun seek(position: Int)

    abstract fun getPosition(): Int
}