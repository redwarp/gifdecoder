package net.redwarp.gif.decoder

sealed class LoopCount {
    object NoLoop : LoopCount()
    object Infinite : LoopCount()
    data class Fixed(val count: Int) : LoopCount()
}
