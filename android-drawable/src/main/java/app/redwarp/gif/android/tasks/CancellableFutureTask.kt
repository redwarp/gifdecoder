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
package app.redwarp.gif.android.tasks

import java.util.concurrent.FutureTask

internal class CancellableFutureTask<V>(private val callable: Cancellable<V>) :
    FutureTask<V>(callable) {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        callable.isCancelled.set(true)
        return super.cancel(mayInterruptIfRunning)
    }
}
