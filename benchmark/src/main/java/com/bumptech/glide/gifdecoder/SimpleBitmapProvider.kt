package com.bumptech.glide.gifdecoder

import android.graphics.Bitmap
import java.util.LinkedList
import java.util.Queue

class SimpleBitmapProvider : GifDecoder.BitmapProvider {
    private val bitmaps = mutableMapOf<Int, Queue<Bitmap>>()
    private val byteArrays = mutableMapOf<Int, Queue<ByteArray>>()
    private val intArrays = mutableMapOf<Int, Queue<IntArray>>()

    override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val key = calculateSize(width, height, config)

        return findBitmap(key)?.also { it.reconfigure(width, height, config) }
            ?: createBitmap(width, height, config)
    }

    override fun release(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        synchronized(this) {
            val queue = bitmaps[bitmap.allocationByteCount]
                ?: LinkedList<Bitmap>().also { bitmaps[bitmap.allocationByteCount] = it }
            queue.offer(bitmap)
        }
    }

    @Synchronized
    override fun release(bytes: ByteArray) {
        byteArrays[bytes.size] ?: LinkedList<ByteArray>().also {
            byteArrays[bytes.size] = it
        }.offer(bytes)
    }

    @Synchronized
    override fun release(array: IntArray) {
        intArrays[array.size] ?: LinkedList<IntArray>().also {
            intArrays[array.size] = it
        }.offer(array)
    }

    @Synchronized
    override fun obtainByteArray(size: Int): ByteArray {
        return byteArrays[size]?.poll() ?: ByteArray(size)
    }

    @Synchronized
    override fun obtainIntArray(size: Int): IntArray {
        return intArrays[size]?.poll() ?: IntArray(size)
    }

    @Synchronized
    override fun flush() {
        intArrays.clear()
        byteArrays.clear()
        bitmaps.iterator().forEach { entry ->
            val queueIterator = entry.value.iterator()
            while (queueIterator.hasNext()) {
                queueIterator.next().recycle()
                queueIterator.remove()
            }
        }
        bitmaps.clear()
    }

    @Synchronized
    private fun findBitmap(cacheKey: Int): Bitmap? {
        return bitmaps[cacheKey]?.poll()
    }

    private fun createBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }

    private fun calculateSize(width: Int, height: Int, config: Bitmap.Config) =
        width * height * config.byteSize()
}

private fun Bitmap.Config.byteSize(): Int {
    return when (this) {
        Bitmap.Config.ALPHA_8 -> 1
        Bitmap.Config.ARGB_8888 -> 4
        Bitmap.Config.RGB_565 -> 2
        else -> throw UnsupportedOperationException("These bitmap formats are not supported")
    }
}
