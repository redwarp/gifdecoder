package app.redwarp.gif

import app.redwarp.gif.decoder.Result

fun <T> Result<T>.unwrap(): T {
    when (this) {
        is Result.Success -> return value
        is Result.Error -> throw Exception(reason)
        else -> throw Exception() // <- da f?
    }
}
