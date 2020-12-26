package net.redwarp.gif.decoder.lzw

class NativeLzwDecoder : LzwDecoder {

    companion object {
        init {
            System.loadLibrary("giflzwdecoder")
        }
    }

    external override fun decode(imageData: ByteArray, destination: ByteArray, pixelCount: Int)

    external fun access(bridge: DecodeJniBridge)

    external fun decodeFull(
        imageData: ByteArray,
        scratch: ByteArray,
        pixels: IntArray,
        colorTable: IntArray,
        transparentColorIndex: Int,
        imageWidth: Int,
        frameWidth: Int,
        frameHeight: Int,
        offsetX: Int,
        offsetY: Int,
        interlaced: Boolean
    )

    external fun fillPixels(
        pixels: IntArray,
        colorData: ByteArray,
        colorTable: IntArray,
        transparentColorIndex: Int,
        imageWidth: Int,
        frameWidth: Int,
        frameHeight: Int,
        offsetX: Int,
        offsetY: Int,
        interlaced: Boolean
    )
}