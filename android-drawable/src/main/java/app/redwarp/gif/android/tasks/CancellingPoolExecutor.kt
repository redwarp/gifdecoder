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

import java.util.concurrent.Callable
import java.util.concurrent.RunnableFuture
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class CancellingPoolExecutor : ThreadPoolExecutor(
    0, Int.MAX_VALUE,
    60L, TimeUnit.SECONDS, SynchronousQueue()
) {
    override fun <T : Any?> newTaskFor(callable: Callable<T>?): RunnableFuture<T> {
        return if (callable is Cancellable) {
            return CancellableFutureTask(callable)
        } else {
            super.newTaskFor(callable)
        }
    }
}
