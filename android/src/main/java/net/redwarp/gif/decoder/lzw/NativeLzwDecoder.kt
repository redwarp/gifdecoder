package net.redwarp.gif.decoder.lzw

class NativeLzwDecoder : LzwDecoder {

    companion object {
        init {
            System.loadLibrary("giflzwdecoder")
        }
    }

    external override fun decode(imageData: ByteArray, destination: ByteArray, pixelCount: Int)
}