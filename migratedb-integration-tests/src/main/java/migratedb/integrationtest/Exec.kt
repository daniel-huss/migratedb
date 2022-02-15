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

package migratedb.integrationtest

import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object Exec {
    private val executor = Executors.newCachedThreadPool {
        Thread(it).apply { isDaemon = true }
    }

    fun <T> async(block: () -> T): CloseableFuture<T> {
        return CloseableFuture(executor.submit(block))
    }

    fun tryAll(vararg blocks: () -> Unit) = tryAll(blocks.toList())

    fun tryAll(blocks: Iterable<() -> Unit>) {
        var thrown: Exception? = null
        blocks.forEach {
            try {
                it()
            } catch (t: Exception) {
                if (t is InterruptedException) Thread.currentThread().interrupt()
                if (thrown == null) thrown = t
                else if (t != thrown) thrown!!.addSuppressed(t)
            }
        }
        thrown?.let { throw it }
    }

    class CloseableFuture<T>(private val f: Future<T>) : Future<T> by f, AutoCloseable, ReadOnlyProperty<Any, T>, () -> T {
        override fun invoke(): T {
            return f.get()
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return f.get()
        }

        override fun close() {
            if (f.isDone) {
                runCatching { (f.get() as? AutoCloseable).use { } }
                    .exceptionOrNull()
                    ?.takeIf { it is InterruptedException }
                    ?.let { Thread.currentThread().interrupt() }
            }
        }
    }
}
