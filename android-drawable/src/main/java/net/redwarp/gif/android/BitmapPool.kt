package net.redwarp.gif.android

import android.graphics.Bitmap
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue

internal class BitmapPool private constructor() {

    private val bitmaps = mutableMapOf<Int, Queue<Bitmap>>()

    fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val key = calculateSize(width, height, config)

        synchronized(this) {
            return findBitmap(key)?.also { it.reconfigure(width, height, config) }
                ?: createBitmap(width, height, config)
        }
    }

    fun release(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.recycle()
            return
        }

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

    private fun createBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }

    private fun calculateSize(width: Int, height: Int, config: Bitmap.Config) =
        width * height * when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            else -> throw UnsupportedOperationException("Only ARGB_8888 and RGB_565 are supported")
        }

    @Synchronized
    fun flush() {
        bitmaps.iterator().forEach { entry ->
            val queueIterator = entry.value.iterator()
            while (queueIterator.hasNext()) {
                queueIterator.next().recycle()
                queueIterator.remove()
            }
        }
        bitmaps.clear()
    }

    protected fun finalize() {
        flush()
    }

    companion object {
        private var sharedBitmapPool: WeakReference<BitmapPool>? = null

        fun obtain(): BitmapPool {
            return sharedBitmapPool?.get() ?: BitmapPool().also {
                sharedBitmapPool = WeakReference(it)
            }
        }
    }
}
