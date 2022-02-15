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
import migratedb.integrationtest.*
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import javax.sql.DataSource

enum class MariaDb(image: String) : DatabaseSupport {
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
            const val defaultDatabase = "mariadb"
        }

        fun dataSource(user: String, databaseAndSchema: String): DataSource {
            return MariaDbDataSource().also {
                it.user = user
                it.setPassword(password)
                it.port = port
                it.databaseName = databaseAndSchema
            }
        }

        init {
            withEnv("MARIADB_USER", regularUser)
            withEnv("MARIADB_PASSWORD", password)
            withEnv("MARIADB_ROOT_PASSWORD", password)
            withExposedPorts(port)
        }
    }

    override fun get(sharedResources: SharedResources): DatabaseSupport.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DatabaseSupport.Handle {
        override val type = MariaDBDatabaseType()

        private val adminDataSource = container().dataSource(Container.adminUser, Container.defaultDatabase)

        override fun createDatabaseIfNotExists(dbName: SafeIdentifier): DataSource {
            return adminDataSource.also { ds ->
                ds.awaitConnectivity().use {
                    createDatabaseIfNotExists(it, dbName)
                }
            }
        }

        private fun createDatabaseIfNotExists(connection: Connection, dbName: SafeIdentifier) {
            connection.createStatement().use { s ->
                s.executeUpdate("create database if not exists $dbName")
            }
        }

        override fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource {
            return container().dataSource(Container.adminUser, "${databaseName}_${schemaName}")
        }

        override fun dropDatabaseIfExists(dbName: SafeIdentifier) {
            adminDataSource.awaitConnectivity().use { connection ->
                connection.createStatement().use { s ->
                    s.executeUpdate("drop database if exists $dbName")
                }
            }
        }

        override fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schemaName, Names.nextTable())
        }

        override fun close() = container.close()
    }
}
