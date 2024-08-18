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

import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SharedResources private constructor() : ExtensionContext.Store.CloseableResource {

    companion object {
        fun ExtensionContext.Store.resources(): SharedResources {
            return getOrComputeIfAbsent(SharedResources::class.java, { SharedResources() }, SharedResources::class.java)
        }

        val MAX_CONTAINERS = System.getenv("MAX_CONTAINERS")?.toIntOrNull() ?: 5
    }

    private val lock = ReentrantLock()
    private var closed = false
    private val network = Network.newNetwork()
    private val containerPool = ContainerPool(MAX_CONTAINERS, IntegrationTest::currentTestInfo)
    private val logConsumersByAlias = LinkedHashMap<String, ToFileLogConsumer>()

    private fun getOrCreateLogConsumer(alias: String): ToFileLogConsumer {
        lock.withLock {
            check(!closed)
            return logConsumersByAlias.computeIfAbsent(alias) { ToFileLogConsumer(alias) }
        }
    }

    fun <T : GenericContainer<*>> container(alias: String, setup: () -> T): Lease<T> {
        val logConsumer = getOrCreateLogConsumer(alias)
        return containerPool.lease(alias) {
            // This code block might be called from a different thread and must not acquire our lock, otherwise
            // deadlocks will occur, that's why the log consumer is built eagerly from the synchronized block.
            setup().also {
                it.withNetworkAliases(alias)
                it.withNetwork(network)
                it.withLogConsumer(logConsumer)
                it.start()
            }
        }
    }

    override fun close() {
        lock.withLock {
            closed = true
            network.use {
                containerPool.use {}
                logConsumersByAlias.values.reversed().forEach { it.close() }
            }
        }
    }
}
