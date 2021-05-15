package app.redwarp.gif.decoder

sealed interface LoopCount {
    object NoLoop : LoopCount
    object Infinite : LoopCount
    data class Fixed(val count: Int) : LoopCount
}
