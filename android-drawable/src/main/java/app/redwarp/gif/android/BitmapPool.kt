package app.redwarp.gif.android

import android.graphics.Bitmap
import java.lang.ref.WeakReference
import java.util.TreeMap

// Reuse the idea from Coil to not use a bitmap that is more than 10 times too big.
private const val MAX_SIZE_MULTIPLE = 4

internal class BitmapPool private constructor() {
    private val bitmaps = Store()

    fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val key = calculateSize(width, height, config)

        synchronized(this) {
            return findBitmap(key)?.also { it.reconfigure(width, height, config) }
                ?: createBitmap(width, height, config)
        }
    }

    fun release(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.recycle()
            return
        }

        synchronized(this) {
            bitmaps.put(bitmap.allocationByteCount, bitmap)
        }
    }

    private fun findBitmap(cacheKey: Int): Bitmap? {
        return bitmaps[cacheKey]
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
        bitmaps.flush {
            it.recycle()
        }
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

    private class Store {
        private val bitmaps = TreeMap<Int, MutableList<Bitmap>>()

        operator fun get(key: Int): Bitmap? {
            val optimizedKey =
                bitmaps.ceilingKey(key)?.takeIf { it < MAX_SIZE_MULTIPLE * key } ?: key

            val entries = bitmaps[optimizedKey] ?: return null

            val bitmap = entries.removeLast()
            if (entries.isEmpty()) {
                // Cleaning up
                bitmaps.remove(optimizedKey)
            }
            return bitmap
        }

        fun put(key: Int, bitmap: Bitmap) {
            bitmaps.getOrPut(key) {
                mutableListOf()
            }.add(bitmap)
        }

        fun flush(forEach: (Bitmap) -> Unit) {
            for (sizeCategory in bitmaps.values) {
                for (entry in sizeCategory) {
                    forEach(entry)
                }
            }
            bitmaps.clear()
        }
    }
}
