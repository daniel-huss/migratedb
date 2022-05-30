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

import migratedb.core.internal.database.mysql.mariadb.MariaDBDatabaseType
import migratedb.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class MariaDb(image: String) : DbSystem {
    V10_2("mariadb:10.2"),
    V10_3("mariadb:10.3"),
    V10_4("mariadb:10.4"),
    V10_5("mariadb:10.5"),
    V10_6("mariadb:10.6"),
    V10_7("mariadb:10.7"),
    ;

    // Relevant idiosyncracies:
    //  - Same as MySQL

    private val containerAlias = "mariadb_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "MariaDB ${name.replace('_', '.')}"

    companion object {
        private const val port = 3306
        private const val password = "test"
        const val adminUser = "root"
        const val regularUser = "mariadb"
        val defaultDatabase = "mariadb".asSafeIdentifier()
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String, database: String): DataSource {
            return MariaDbDataSource().also {
                it.user = user
                it.setPassword(password)
                it.port = getMappedPort(port)
                it.databaseName = database
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(300_000_000)
            }
            withEnv("MARIADB_USER", regularUser)
            withEnv("MARIADB_PASSWORD", password)
            withEnv("MARIADB_ROOT_PASSWORD", password)
            withEnv("MARIADB_DATABASE", defaultDatabase.toString())
            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = MariaDBDatabaseType()

        private val internalDs = container().dataSource(adminUser, defaultDatabase.toString())

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            internalDs.work {
                it.update("create database if not exists ${normalizeCase(namespace)}")
            }
            return namespace
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(adminUser, normalizeCase(namespace).toString())
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            require(namespace != defaultDatabase) { "Cannot drop the default database" }
            internalDs.work {
                it.update("drop database if exists ${normalizeCase(namespace)}")
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
