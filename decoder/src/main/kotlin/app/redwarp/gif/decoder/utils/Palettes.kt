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
package app.redwarp.gif.decoder.utils

/**
 * Fallback palettes used when a GIF has neither global nor local color tables.
 */
object Palettes {
    /**
     * The 16 color table used on the Apple II computer.
     * See [Apple II graphics](https://en.wikipedia.org/wiki/Apple_II_graphics)
     */
    private val apple2 by lazy {
        intArrayOf(
            0xff000000.toInt(),
            0xff6c2940.toInt(),
            0xff403578.toInt(),
            0xffd93cf0.toInt(),
            0xff135740.toInt(),
            0xff808080.toInt(),
            0xff2697f0.toInt(),
            0xffbfb4f8.toInt(),
            0xff404b07.toInt(),
            0xffd9680f.toInt(),
            0xff808080.toInt(),
            0xffeca8bf.toInt(),
            0xff26c30f.toInt(),
            0xffbfca87.toInt(),
            0xff93d6bf.toInt(),
            0xffffffff.toInt(),
        )
    }

    /**
     * A black and white palette, for 2 colored GIFs.
     */
    private val blackAndWhite by lazy {
        intArrayOf(0xff000000.toInt(), 0xffffffff.toInt())
    }

    fun createFakeColorMap(size: Int): IntArray {
        return if (size == 2) {
            blackAndWhite
        } else {
            val colors = IntArray(size)
            for (index in 0 until size) {
                val equivalentIndex = ((index * apple2.size) / size).coerceIn(0, apple2.size - 1)
                colors[index] = apple2[equivalentIndex]
            }
            colors
        }
    }
}
