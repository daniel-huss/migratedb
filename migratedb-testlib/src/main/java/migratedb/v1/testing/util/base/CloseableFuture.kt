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

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CloseableFuture<T>(
    private val waitOnClose: Boolean,
    private val valueIsCloseable: Boolean,
    private val future: Future<T>
) : Future<T> by future, AutoCloseable, ReadOnlyProperty<Any, T>, () -> T {
    private val closed = AtomicBoolean(false)

    override fun invoke(): T {
        return future.get()
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return future.get()
    }

    override fun close() {
        if (valueIsCloseable && closed.compareAndSet(false,true)) {
            if (waitOnClose) handleInterruptionOnly {
                (future.get() as AutoCloseable).close()
            } else if (future.isDone) {
                handleInterruptionOnly {
                    (future.get() as AutoCloseable).close()
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
