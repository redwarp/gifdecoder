package app.redwarp.gif.decoder.utils

import java.io.InputStream

internal fun InputStream.readShortLe(): Short {
    return (read() or (read() shl 8)).toShort()
}

internal fun InputStream.readByte(): Byte = read().toByte()

internal fun InputStream.readAsciiString(byteCount: Int): String {
    val buffer = StringBuilder()
    repeat(byteCount) {
        buffer.append(read().toChar())
    }
    return buffer.toString()
}
