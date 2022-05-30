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

package migratedb.integrationtest.database

import migratedb.core.api.internal.database.base.DatabaseType
import migratedb.core.internal.database.cockroachdb.CockroachDBDatabaseType
import migratedb.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class CockroachDb(image: String) : DbSystem {
    V21_2_6("cockroachdb/cockroach:v21.2.6"),
    // Bug: MigrateDB tries to SET ROLE but this version doesn't support that feature! V20_2_0("cockroachdb/cockroach:v20.2.0"),
    // Lower versions are EOL, should we test them?
    ;

    // Relevant idiosyncracies:
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
        fun dataSource(databaseName: String = defaultDatabase.toString(), currentSchema: String? = null): DataSource {
            return PGSimpleDataSource().also {
                it.user = adminUser
                it.portNumbers = intArrayOf(getMappedPort(port))
                it.applicationName = "MigrateDB Integration Test"
                it.loggerLevel = "OFF"
                it.serverNames = arrayOf(host)
                it.databaseName = databaseName
                it.currentSchema = currentSchema
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.withCmd("start", "--insecure", "--join=localhost:$port")
                it.hostConfig!!.withMemory(300_000_000)
            }
            withExposedPorts(port)
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

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type: DatabaseType = CockroachDBDatabaseType()
        private val internalDs = container().dataSource()

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

        override fun close() = container.close()
    }
}
