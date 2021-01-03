package net.redwarp.gif.android

import java.util.LinkedList

class SizeLimitedQueue<E>(private val capacity: Int) : LinkedList<E>() {
    override fun add(element: E): Boolean {
        if (size >= capacity) removeFirst()
        return super.add(element)
    }
}