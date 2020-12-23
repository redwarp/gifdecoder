package net.redwarp.gif.decoder.android

import android.graphics.Bitmap
import java.util.*

internal class BitmapCache() {

    private val bitmaps = mutableMapOf<Int, Queue<Bitmap>>()

    fun obtain(width: Int, height: Int): Bitmap {
        val key = calculateSize(width, height)

        return findBitmap(key)?.also { it.reconfigure(width, height, Bitmap.Config.ARGB_8888) }
            ?: createBitmap(width, height)
    }

    fun release(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        synchronized(this) {
            val queue = bitmaps[bitmap.allocationByteCount]
                ?: LinkedList<Bitmap>().also { bitmaps[bitmap.allocationByteCount] = it }
            queue.offer(bitmap)
        }
    }

    @Synchronized
    private fun findBitmap(cacheKey: Int): Bitmap? {
        return bitmaps[cacheKey]?.poll()
    }

    private fun createBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun calculateSize(width: Int, height: Int) =
        width * height * 4
}