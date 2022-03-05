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
package migratedb.integrationtest.util.container

import migratedb.integrationtest.util.base.Exec.tryAll
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Pools containers for shared use, so the same container can be used by multiple threads, but only [size] containers
 * may be running at any point in time. When a container is unused by any thread, it may be closed to make room for
 * other containers. "Using" a container is represented by holding an instance of [Lease], which must be closed when
 * you're done using the container.
 */
class ContainerPool(private val size: Int) : AutoCloseable {

    init {
        check(size > 0)
    }

    private class LeaseImpl<T : AutoCloseable>(val slot: Slot<T>) : Lease<T> {
        private var closed = false
        override fun close() = synchronized(this) {
            if (closed) return
            closed = true
            slot.unlease()
        }

        override fun invoke() = synchronized(this) {
            check(!closed)
            slot.container
        }
    }

    private inner class Slot<T : AutoCloseable>(containerInitializer: () -> T) : AutoCloseable {
        private val lazyContainer = lazy(this, containerInitializer)

        // @GuardedBy("leaseLock")
        private var leases: Int = 0
        private var closed = false

        val container get() = lazyContainer.value

        fun lease(): LeaseImpl<T> = leaseLock.withLock {
            leases++
            LeaseImpl(this)
        }

        fun leases() = leases

        fun unlease() = leaseLock.withLock {
            leases = (leases - 1).coerceAtLeast(0)
            if (leases == 0) maybeOneUnusedSlot.signalAll()
        }

        override fun close() = synchronized(this) {
            if (closed) return
            closed = true
            if (lazyContainer.isInitialized()) {
                lazyContainer.value.close()
            }
        }
    }

    private var closed = false
    private val leaseLock = ReentrantLock()
    private val slots = HashMap<String, Slot<*>>()
    private val maybeOneUnusedSlot = leaseLock.newCondition()

    @Suppress("UNCHECKED_CAST")
    fun <T : AutoCloseable> lease(alias: String, containerInitializer: () -> T): Lease<T> {
        leaseLock.withLock {
            check(!closed)
            slots[alias]?.let { return it.lease() as Lease<T> }
            while (slots.size >= size) {
                if (!removeOneSlotWithoutLeases()) {
                    maybeOneUnusedSlot.await()
                }
                check(!closed) // re-check needed after waiting for condition
            }
            val newSlot = Slot(containerInitializer)
            slots[alias] = newSlot
            return newSlot.lease()
        }
    }

    private fun removeOneSlotWithoutLeases(): Boolean {
        var oneWasRemoved = false
        val iter = slots.iterator()
        while (iter.hasNext() && !oneWasRemoved) {
            if (iter.next().value.leases() == 0) {
                iter.remove()
                oneWasRemoved = true
            }
        }
        return oneWasRemoved
    }


    override fun close() {
        leaseLock.withLock {
            if (closed) return
            closed = true
            tryAll(slots.values.map { { it.close() } })
            maybeOneUnusedSlot.signalAll()
        }
    }
}
