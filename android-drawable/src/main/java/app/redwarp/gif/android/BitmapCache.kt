/* Copyright 2020 Benoit Vermont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.redwarp.gif.android

import android.graphics.Bitmap
import java.util.TreeMap

// Reuse the idea from Coil to not use a bitmap that is more than 10 times too big.
private const val MAX_SIZE_MULTIPLE = 4
private const val BYTE_PER_PIXEL = 4

internal class BitmapCache {
    private val bitmaps = Store()

    fun obtain(width: Int, height: Int): Bitmap {
        val key = calculateSize(width, height)

        synchronized(this) {
            return findBitmap(key)?.also { it.reconfigure(width, height, Bitmap.Config.ARGB_8888) }
                ?: createBitmap(width, height)
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
            bitmaps[bitmap.allocationByteCount] = bitmap
        }
    }

    private fun findBitmap(cacheKey: Int): Bitmap? = bitmaps[cacheKey]

    private fun createBitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private fun calculateSize(width: Int, height: Int) = width * height * BYTE_PER_PIXEL

    @Synchronized
    fun flush() {
        bitmaps.flush()
    }

    protected fun finalize() {
        flush()
    }

    private class Store {
        companion object {
            const val RECLAIM_COUNT = 100
        }

        private val bitmaps = TreeMap<Int, MutableList<Bitmap>>()
        private var setCounter = 0

        operator fun get(key: Int): Bitmap? {
            val optimizedKey =
                bitmaps.ceilingKey(key)?.takeIf { it < MAX_SIZE_MULTIPLE * key } ?: key

            val entries = bitmaps[optimizedKey] ?: return null

            if (entries.isEmpty()) return null

            return entries.removeLast()
        }

        operator fun set(key: Int, bitmap: Bitmap) {
            bitmaps.getOrPut(key) {
                mutableListOf()
            }.add(bitmap)

            setCounter++
            if (setCounter > RECLAIM_COUNT) {
                setCounter = 0

                consolidate()
            }
        }

        fun flush() {
            for (sizeCategory in bitmaps.values) {
                for (entry in sizeCategory) {
                    entry.recycle()
                }
            }
            bitmaps.clear()
        }

        /**
         * Deletes empty entries, to unclog the bitmaps [TreeMap].
         */
        private fun consolidate() {
            val entries = bitmaps.entries.iterator()
            while (entries.hasNext()) {
                val entry = entries.next()
                if (entry.value.isEmpty()) {
                    entries.remove()
                }
            }
        }
    }
}
