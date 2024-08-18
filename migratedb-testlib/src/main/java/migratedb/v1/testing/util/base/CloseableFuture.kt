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
