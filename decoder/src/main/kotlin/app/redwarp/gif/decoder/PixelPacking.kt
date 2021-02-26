package app.redwarp.gif.decoder

/**
 * What type of pixel packing should be used when decoding the color tables.
 */
enum class PixelPacking {
    /**
     * Standard alpha red green and blue.
     */
    ARGB,

    /**
     * Alpha, blue, green and red. Useful for Android Bitmap, as they use this packing internally for ARGB_8888 format.
     */
    ABGR
}
