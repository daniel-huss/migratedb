/*
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.integrationtest.util.container

import com.google.common.collect.ConcurrentHashMultiset
import migratedb.v1.testing.util.base.async
import migratedb.v1.testing.util.base.daemonThreadFactory
import migratedb.v1.testing.util.base.tryAll
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

/**
 * Pools containers for shared use, so the same container can be used by multiple threads, but only up to [size]
 * containers may be running at any point in time. When a container is unused by any thread, it may be closed to make
 * room for other containers. "Using" a container is represented by holding onto a [Lease], which must be closed when
 * you're done using the container.
 */
class ContainerPool(private val size: Int) : AutoCloseable {

    init {
        check(size > 0)
    }

    private var debug = true
    private var closed = false
    private val requestedSlots = ConcurrentHashMultiset.create<String>()
    private val slotLock = ReentrantReadWriteLock(true)
    private val slotsByName = HashMap<String, Slot<*>>()
    private val slotHasBeenReaped = slotLock.writeLock().newCondition()
    private val reaper = Reaper()

    private fun debug(msg: () -> String) {
        if (debug) {
            System.err.println("[${Thread.currentThread().name}] ${msg()}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : AutoCloseable> lease(name: String, containerInitializer: () -> T): Lease<T> {
        requestedSlots.add(name)
        try {
            while (true) {
                slotLock.read {
                    check(!closed)
                    slotsByName[name]?.let { return it.lease() as Lease<T> }
                    slotLock.write {
                        check(!closed) // re-check needed after upgrading lock
                        slotsByName[name]?.let { return it.lease() as Lease<T> }
                        if (slotsByName.size < size) {
                            val newSlot = Slot(name, containerInitializer)
                            slotsByName[name] = newSlot
                            return newSlot.lease()
                        } else {
                            slotHasBeenReaped.await(1, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        } finally {
            requestedSlots.remove(name)
        }
    }

    override fun close() {
        slotLock.write {
            if (closed) return
            closed = true
            reaper.use {
                tryAll(slotsByName.values.map { fun() { it.close() } })
                slotsByName.clear()
            }
        }
    }

    private class LeaseFromSlot<T : AutoCloseable>(val slot: Slot<T>) : Lease<T> {
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

    private inner class Slot<T : AutoCloseable>(
        val name: String,
        containerInitializer: () -> T
    ) : AutoCloseable {
        // @GuardedBy("leaseLock")
        private var leases: Int = 0
        private val futureContainer = async<AutoCloseable>(waitOnClose = true, containerInitializer)
        private var closed: Boolean = false
        private var idleStart: Instant? = null

        @Suppress("UNCHECKED_CAST")
        val container
            get() = futureContainer.get() as T

        val idleTime: Duration?
            get() = slotLock.read {
                when (leases) {
                    0 -> idleStart?.let { Duration.between(it, Instant.now()) }
                    else -> null
                }
            }

        fun lease(): LeaseFromSlot<T> = slotLock.write {
            debug { "Leasing $name" }
            check(!closed)
            idleStart = Instant.now()
            leases++
            LeaseFromSlot(this)
        }

        fun unlease() = slotLock.write {
            debug { "Unleasing $name" }
            idleStart = Instant.now()
            leases = (leases - 1).coerceAtLeast(0)
            if (leases == 0) {
                debug { "Slot $name now has zero leases" }
                reaper.gazeUpon(this)
            }
        }

        override fun close() = slotLock.write {
            if (!closed) {
                debug { "Closing slot $name" }
                futureContainer.close()
                closed = true
            }
        }
    }

    private inner class Reaper : AutoCloseable {
        private val lock = ReentrantLock()
        private val timeToWakeUp = lock.newCondition()
        private val toCheck = HashSet<Slot<*>>()
        private var scanSlots = true
        private val executor = Executors.newSingleThreadExecutor(daemonThreadFactory).also {
            it.submit(this::reaperCycle)
        }

        private fun reaperCycle() {
            while (!Thread.currentThread().isInterrupted) {
                lock.withLock {
                    timeToWakeUp.await(5, TimeUnit.SECONDS)
                }
                debug { "Requested slots: $requestedSlots" }
                reapIdleSlots()
            }
        }

        private fun reapIdleSlots() {
            var reapCount = 0

            fun reap(slot: Slot<*>) {
                debug { "Reaping slot ${slot.name} which has been idle for ${slot.idleTime}" }
                slotsByName.remove(slot.name)
                slot.close()
                slotHasBeenReaped.signal()
                reapCount++
            }

            slotLock.write {
                if (slotsByName.size < size) {
                    debug { "Pool is not at capacity, nothing to reap" }
                    return
                }

                if (scanSlots) {
                    slotsByName.values.asSequence()
                        .mapNotNull { slot -> slot.idleTime?.let { slot to it } }
                        .sortedByDescending { (_, idleTime) -> idleTime }
                        .take(requestedSlots.elementSet().size.coerceAtLeast(1))
                        .toList() // -> the map must not be modified while iterating over it
                        .forEach { (slot, _) ->
                            reap(slot)
                        }
                } else {
                    for (slot in toCheck) {
                        slot.idleTime?.takeIf { it.toSeconds() >= 1 }?.let {
                            reap(slot)
                        }
                    }
                    toCheck.clear()
                    scanSlots = true // Will be set to false by gazeUpon() if called before the next wake-up
                }

                debug { "Waking up the reaper resulted in $reapCount slots being reaped" }
            }
        }

        fun gazeUpon(slot: Slot<*>) {
            lock.withLock {
                toCheck.add(slot)
                scanSlots = false
                timeToWakeUp.signal()
            }
        }

        override fun close() {
            executor.shutdownNow()
        }
    }
}
