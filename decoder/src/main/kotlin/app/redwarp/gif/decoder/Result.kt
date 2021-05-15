package app.redwarp.gif.decoder

sealed interface Result<T> {
    class Success<T>(val value: T) : Result<T>
    data class Error<T>(val reason: String) : Result<T>

    fun <U> map(block: (T) -> U): Result<U> {
        return when (this) {
            is Success -> Success(block(value))
            is Error -> Error(reason)
        }
    }
}
