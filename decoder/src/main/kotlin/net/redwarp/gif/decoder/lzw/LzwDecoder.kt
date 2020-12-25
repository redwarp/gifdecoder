package net.redwarp.gif.decoder.lzw

interface LzwDecoder {
    fun decode(imageData: ByteArray, destination: ByteArray, pixelCount: Int)
}