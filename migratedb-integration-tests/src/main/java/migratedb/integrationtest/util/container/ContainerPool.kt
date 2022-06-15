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

import migratedb.testing.util.base.Exec
import migratedb.testing.util.base.Exec.async
import migratedb.testing.util.base.Exec.tryAll
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

/**
 * Pools containers for shared use, so the same container can be used by multiple threads, but only up to [size] containers
 * may be running at any point in time. When a container is unused by any thread, it may be closed to make room for
 * other containers. "Using" a container is represented by holding onto a [Lease], which must be closed when
 * you're done using the container.
 */
class ContainerPool(private val size: Int) : AutoCloseable {

    init {
        check(size > 3)
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
        // @GuardedBy("leaseLock")
        private var leases: Int = 0
        private val futureContainer = async<AutoCloseable>(waitOnClose = true, containerInitializer)
        private var closed: Boolean = false
        private var idleStart: Instant? = null

        @Suppress("UNCHECKED_CAST")
        val container
            get() = futureContainer.get() as T

        val idleSince: Duration?
            get() = slotLock.read {
                when (leases) {
                    0 -> idleStart?.let { Duration.between(it, Instant.now()) }
                    else -> null
                }
            }

        fun lease(): LeaseImpl<T> = slotLock.write {
            check(!closed)
            idleStart = Instant.now()
            leases++
            LeaseImpl(this)
        }

        fun unlease() = slotLock.write {
            idleStart = Instant.now()
            leases = (leases - 1).coerceAtLeast(0)
            if (leases == 0) {
                mayHaveFreeSlots.signalAll()
            }
        }

        override fun close() = slotLock.write {
            if (!closed) {
                futureContainer.close()
                closed = true
            }
        }
    }

    private inner class Reaper : AutoCloseable {
        private val lock = ReentrantLock()
        private val timeToWakeUp = lock.newCondition()
        private val executor = Executors.newSingleThreadExecutor(Exec.threadFactory).also {
            it.submit(this::reaperCycle)
        }

        private fun reaperCycle() {
            while (!Thread.currentThread().isInterrupted) {
                lock.withLock {
                    timeToWakeUp.await(5, TimeUnit.SECONDS)
                }
                reapIdleSlots()
            }
        }

        private fun reapIdleSlots() {
            slotLock.write {
                if (slotsByName.size < size) return
                val slotsToKill = slotsByName.asSequence()
                    .mapNotNull { (name, slot) -> slot.idleSince?.let { it to name } }
                    .filter { (idleTime, _) -> idleTime > Duration.ofSeconds(1) }
                    .sortedByDescending { (idleTime, _) -> idleTime }
                    .take(requestedSlots.get().coerceAtLeast(1))
                    .map { (_, name) -> name }
                    .toList()

                slotsToKill.forEach {
                    slotsByName.remove(it)?.close()
                    mayHaveFreeSlots.signal()
                }
            }
        }

        fun wakeUp() {
            lock.withLock {
                timeToWakeUp.signalAll()
            }
        }

        override fun close() {
            executor.shutdownNow()
        }
    }

    private var closed = false
    private val requestedSlots = AtomicInteger(0)
    private val slotLock = ReentrantReadWriteLock()
    private val slotsByName = HashMap<String, Slot<*>>()
    private val mayHaveFreeSlots = slotLock.writeLock().newCondition()
    private val reaper = Reaper()

    @Suppress("UNCHECKED_CAST")
    fun <T : AutoCloseable> lease(name: String, containerInitializer: () -> T): Lease<T> {
        requestedSlots.incrementAndGet()
        try {
            while (true) {
                slotLock.read {
                    check(!closed)
                    slotsByName[name]?.let { return it.lease() as Lease<T> }
                    reaper.wakeUp()
                    slotLock.write {
                        check(!closed) // re-check needed after upgrading lock
                        slotsByName[name]?.let { return it.lease() as Lease<T> }
                        if (slotsByName.size < size) {
                            val newSlot = Slot(containerInitializer)
                            slotsByName[name] = newSlot
                            return newSlot.lease()
                        } else {
                            mayHaveFreeSlots.await(1, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        } finally {
            requestedSlots.decrementAndGet()
        }
    }


    override fun close() {
        slotLock.write {
            if (closed) return
            closed = true
            reaper.use {
                tryAll(slotsByName.values.map { { it.close() } })
                mayHaveFreeSlots.signalAll()
            }
        }
    }
}
