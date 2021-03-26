package app.redwarp.gif.decoder

fun <T> Result<T>.unwrap(): T {
    when (this) {
        is Result.Success -> return value
        is Result.Error -> throw Exception(reason)
    }
}