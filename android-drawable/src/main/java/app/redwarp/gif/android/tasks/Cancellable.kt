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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Inspired by [https://codeahoy.com/java/Cancel-Tasks-In-Executors-Threads],
 * allowing our callable to be marked as cancelled.
 */
internal abstract class Cancellable<V> : Callable<V> {
    val isCancelled: AtomicBoolean = AtomicBoolean(false)
}
