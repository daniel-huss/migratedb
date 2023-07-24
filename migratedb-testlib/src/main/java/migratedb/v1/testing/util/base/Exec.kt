/*
 * Copyright 2022 The MigrateDB contributors
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

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

object Exec {
    val threadFactory = object : ThreadFactory {
        private val delegate = Executors.defaultThreadFactory()
        override fun newThread(r: Runnable) = delegate.newThread(r).also {
            it.isDaemon = true
        }
    }

    @PublishedApi
    internal val executor: ExecutorService = Executors.newCachedThreadPool(threadFactory)

    inline fun <reified T> async(waitOnClose: Boolean = false, noinline block: () -> T): CloseableFuture<T> {
        return CloseableFuture(waitOnClose, T::class.isSubclassOf(AutoCloseable::class), executor.submit(block))
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

    class CloseableFuture<T>(
        private val waitOnClose: Boolean,
        private val valueIsCloseable: Boolean,
        private val f: Future<T>
    ) : Future<T> by f, AutoCloseable, ReadOnlyProperty<Any, T>, () -> T {
        private val closed = AtomicBoolean(false)

        override fun invoke(): T {
            return f.get()
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return f.get()
        }

        override fun close() {
            if (valueIsCloseable && !closed.getAndSet(true)) {
                if (waitOnClose) handleInterruptionOnly {
                    (f.get() as AutoCloseable).close()
                } else if (f.isDone) {
                    handleInterruptionOnly {
                        (f.get() as AutoCloseable).close()
                    }
                }
            }
        }

        private fun handleInterruptionOnly(block: () -> Unit) {
            try {
                block()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Exception) {
            }
        }
    }
}
