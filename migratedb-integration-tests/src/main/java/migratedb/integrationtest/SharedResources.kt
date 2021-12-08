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

import migratedb.integrationtest.Exec.async
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class SharedResources private constructor() : ExtensionContext.Store.CloseableResource {
    companion object {
        fun ExtensionContext.Store.resources(): SharedResources {
            return getOrComputeIfAbsent(SharedResources::class.java, { SharedResources() }, SharedResources::class.java)
        }
    }

    private val lock = object : Any() {}
    private var closed = false
    private val network = Network.newNetwork()
    private val maxContainers = 10
    private val containers = object : LinkedHashMap<Any, AutoCloseable>(maxContainers) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, AutoCloseable>): Boolean {
            return if (size >= maxContainers) {
                eldest.value.close()
                true
            } else false
        }
    }

    private data class ContainerKey(val alias: String)
    private data class LogConsumerKey(val alias: String)

    @Suppress("UNCHECKED_CAST")
    private fun <X : AutoCloseable> getOrCreate(key: Any, supplier: () -> X): X = synchronized(lock) {
        if (closed) throw IllegalStateException("Closed")
        containers.getOrPut(key) { supplier() } as X
    }

    fun <T : GenericContainer<*>> container(alias: String) = synchronized(lock) {
        @Suppress("UNCHECKED_CAST")
        containers[ContainerKey(alias)] as? T
    }

    fun <T : GenericContainer<*>> container(alias: String, setup: () -> T): T {
        return getOrCreate(ContainerKey(alias)) {
            setup().also {
                it.withNetworkAliases(alias)
                it.withNetwork(network)
                it.withLogConsumer(getOrCreate(LogConsumerKey(alias)) { ToFileLogConsumer(alias) })
                it.start()
            }
        }
    }


    override fun close() {
        synchronized(lock) {
            closed = true
            network.use {
                containers.values.reversed()
                    .asSequence()
                    .map { async { it.close() } }
                    .forEach { it() }
            }
        }
    }
}
