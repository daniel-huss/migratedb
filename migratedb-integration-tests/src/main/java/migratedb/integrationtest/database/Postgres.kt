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
import migratedb.core.internal.database.postgresql.PostgreSQLDatabaseType
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
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class Postgres(image: String) : DbSystem {
    V9_6("postgres:9.6-alpine"),
    V10("postgres:10-alpine"),
    V11("postgres:11-alpine"),
    V12("postgres:12-alpine"),
    V13("postgres:13-alpine"),
    V14("postgres:14-alpine"),
    ;

    // Relevant idiosyncracies:
    //  - Normalizes identifiers to lower case instead of upper case

    private val containerAlias = "postgres_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "PostgreSQL ${name.replace('_', '.')}"

    companion object {
        const val port = 5432
        const val password = "test"
        const val adminUser = "postgres"
        val defaultDatabase = "postgres".asSafeIdentifier()
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(databaseName: String = defaultDatabase.toString(), currentSchema: String? = null): DataSource {
            return PGSimpleDataSource().also {
                it.user = adminUser
                it.password = password
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
                it.hostConfig!!.withMemory(300_000_000)
            }
            withEnv("POSTGRES_PASSWORD", password)
            withCreateContainerCmdModifier {
                it.withCmd("-c", "fsync=off", "-c", "log_destination=stderr", "-c", "log_statement=all")
            }
            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type: DatabaseType = PostgreSQLDatabaseType()
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
