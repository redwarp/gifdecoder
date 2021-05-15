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

object Palettes {
    private val Apple2 = intArrayOf(
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
        0xffffffff.toInt()
    )

    fun createFakeColorMap(size: Int): IntArray {
        return if (size == 2) {
            intArrayOf(0xff000000.toInt(), 0xffffffff.toInt())
        } else {
            val colors = IntArray(size)
            for (index in 0 until size) {
                val equivalentIndex = ((index * Apple2.size) / size).coerceIn(0, Apple2.size - 1)
                colors[index] = Apple2[equivalentIndex]
            }
            colors
        }
    }
}
