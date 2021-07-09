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

    fun unwrap(): T {
        when (this) {
            is Success -> return value
            is Error -> throw Exception(reason)
        }
    }
}
