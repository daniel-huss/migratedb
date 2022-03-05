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
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.base.awaitConnectivity
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.GenericContainer
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

    private val containerAlias = "mariadb_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "MariaDB $name"

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        companion object {
            private const val port = 3306
            private const val password = "test"
            const val adminUser = "root"
            const val regularUser = "mariadb"
            val defaultDatabase = "mariadb".asSafeIdentifier()
        }

        fun dataSource(user: String, database: String): DataSource {
            return MariaDbDataSource().also {
                it.user = user
                it.setPassword(password)
                it.port = getMappedPort(port)
                it.databaseName = database
            }
        }

        init {
            withEnv("MARIADB_USER", regularUser)
            withEnv("MARIADB_PASSWORD", password)
            withEnv("MARIADB_ROOT_PASSWORD", password)
            withEnv("MARIADB_DATABASE", defaultDatabase.toString())
            withExposedPorts(port)
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = MariaDBDatabaseType()

        private val internalDs = container().dataSource(Container.adminUser, Container.defaultDatabase.toString())

        override fun createDatabaseIfNotExists(databaseName: SafeIdentifier): DataSource {
            internalDs.awaitConnectivity().use { connection ->
                connection.work {
                    it.update("create database if not exists $databaseName")
                }
            }
            return container().dataSource(Container.adminUser, "$databaseName")
        }

        override fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource {
            return container().dataSource(Container.adminUser, "$databaseName")
        }

        override fun dropDatabaseIfExists(databaseName: SafeIdentifier) {
            require(databaseName != Container.defaultDatabase) { "Cannot drop the default database" }
            internalDs.awaitConnectivity().use { connection ->
                connection.work {
                    it.update("drop database if exists $databaseName")
                }
            }
        }

        override fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schemaName, Names.nextTable())
        }

        override fun createSchemaIfNotExists(databaseName: SafeIdentifier, schemaName: SafeIdentifier): SafeIdentifier? = null

        override fun close() = container.close()
    }
}
