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

package migratedb.v1.integrationtest.database

import migratedb.v1.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import migratedb.v1.core.api.internal.database.base.DatabaseType
import migratedb.v1.core.internal.database.cockroachdb.CockroachDBDatabaseType
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.Semaphore
import javax.sql.DataSource

enum class CockroachDb(image: String) : DbSystem {
    V23_1_5("cockroachdb/cockroach:v23.1.5"),
    V22_2_11("cockroachdb/cockroach:v22.2.11"),
    V21_2_17("cockroachdb/cockroach:v21.2.17"),
    // Lower versions are EOL, should we test them?
    ;

    // Relevant idiosyncrasies:
    //  - Normalizes identifiers to lower case instead of upper case
    //  - Tries to be compatible with Postgres, but many features are not implemented

    private val containerAlias = "cockroachdb_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "CockroachDB ${name.replace('_', '.')}"

    companion object {
        const val port = 26257
        const val adminUser = "root"
        val defaultDatabase = "postgres".asSafeIdentifier()
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(databaseName: String = CockroachDb.defaultDatabase.toString(), currentSchema: String? = null): DataSource {
            return PGSimpleDataSource().also {
                it.user = CockroachDb.adminUser
                it.portNumbers = intArrayOf(getMappedPort(CockroachDb.port))
                it.applicationName = "MigrateDB Integration Test"
                it.serverNames = arrayOf(host)
                it.databaseName = databaseName
                it.currentSchema = currentSchema
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.withCmd("start", "--insecure", "--join=localhost:${CockroachDb.port}")
                it.hostConfig!!.withMemory(300_000_000)
            }
            withExposedPorts(CockroachDb.port)
            waitingFor(object : HostPortWaitStrategy() {

                override fun waitUntilReady() {
                    super.waitUntilReady()
                    initCluster()
                }

                private fun initCluster() {
                    val result = execInContainer("./cockroach", "init", "--insecure")
                    check(result.exitCode == 0) {
                        result.stdout + "\n" + result.stderr
                    }
                }
            })
        }
    }

    // No locking implemented atm, so concurrent execution would lead to serialization errors
    private val permits = Semaphore(1)

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        permits.acquire()
        return Handle(sharedResources.container(containerAlias) {
            Container(
                image
            )
        })
    }

    private inner class Handle(private val container: Lease<Container>) :
        DbSystem.Handle {
        override val type: DatabaseType = CockroachDBDatabaseType()
        private val internalDs = container().dataSource()
        private var closed = false

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            internalDs.work {
                it.update("create schema if not exists $namespace")
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            internalDs.work {
                it.update("drop schema if exists $namespace cascade")
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(currentSchema = namespace.toString())
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun normalizeCase(s: CharSequence) = s.toString().lowercase()

        override fun close() {
            synchronized(this) {
                if (closed) return
                closed = true
                container.use {
                    permits.release()
                }
            }
        }
    }
}
