/*
 * Copyright 2022-2024 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.v1.testing.util.base

import java.util.concurrent.*
import kotlin.reflect.full.isSubclassOf

val daemonThreadFactory = object : ThreadFactory {
    private val delegate = Executors.defaultThreadFactory()
    override fun newThread(r: Runnable) = delegate.newThread(r).also {
        it.isDaemon = true
    }
}

@PublishedApi
internal val executor: ExecutorService = Executors.newCachedThreadPool(daemonThreadFactory)

inline fun <reified T : Any> async(
    waitOnClose: Boolean = false,
    noinline block: () -> T
): CloseableFuture<T> {
    val blockAsCallable = Callable { block() }
    return CloseableFuture(
        waitOnClose = waitOnClose,
        valueIsCloseable = T::class.isSubclassOf(AutoCloseable::class),
        future = executor.submit(blockAsCallable)
    )
}

fun tryAll(vararg blocks: () -> Unit) = tryAll(blocks.toList())

fun tryAll(blocks: Iterable<() -> Unit>) {
    var thrown: Exception? = null
    blocks.forEach {
        try {
            it()
        } catch (t: Exception) {
            if (t is InterruptedException) Thread.currentThread().interrupt()
            if (thrown == null) thrown = t else thrown!!.addSuppressed(t)
        }
    }
    thrown?.let { throw it }
}

