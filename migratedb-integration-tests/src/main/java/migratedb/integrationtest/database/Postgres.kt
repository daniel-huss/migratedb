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

import migratedb.core.internal.database.DatabaseType
import migratedb.core.internal.database.postgresql.PostgreSQLDatabaseType
import migratedb.integrationtest.*
import migratedb.integrationtest.SafeIdentifier.Companion.asSafeIdentifier
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import javax.sql.DataSource

enum class Postgres(image: String) : DatabaseSupport {
    V9_6("postgres:9.6-alpine"), V10("postgres:10-alpine"), V11("postgres:11-alpine"), V12("postgres:12-alpine"), V13("postgres:13-alpine"), V14(
        "postgres:14-alpine"
    ), ;

    private val containerAlias = "postgres_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "PostgreSQL $name"

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        companion object {
            const val port = 5432
            const val password = "test"
            const val adminUser = "postgres"
            val defaultDatabase = "postgres".asSafeIdentifier()
        }

        fun dataSource(databaseName: String, schemaName: String? = null): DataSource {
            return PGSimpleDataSource().also {
                it.user = adminUser
                it.password = password
                it.portNumbers = intArrayOf(getMappedPort(port))
                it.applicationName = "MigrateDB Integration Test"
                it.loggerLevel = "OFF"
                it.serverNames = arrayOf(host)
                it.databaseName = databaseName
                it.currentSchema = schemaName
            }
        }

        init {
            withEnv("POSTGRES_PASSWORD", password)
            withCreateContainerCmdModifier {
                it.withCmd("-c", "fsync=off", "-c", "log_destination=stderr", "-c", "log_statement=all")
            }
            withExposedPorts(port)
        }
    }

    override fun get(sharedResources: SharedResources): DatabaseSupport.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DatabaseSupport.Handle {
        override val type: DatabaseType = PostgreSQLDatabaseType()


        override fun createDatabaseIfNotExists(dbName: SafeIdentifier): DataSource {
            check(dbName != Container.defaultDatabase) { "Tests cannot use the default database" }
            return container().dataSource(Container.defaultDatabase.toString()).also { ds ->
                ds.awaitConnectivity().use {
                    createDatabaseIfNotExists(it, dbName)
                }
            }
        }

        private fun createDatabaseIfNotExists(connection: Connection, dbName: SafeIdentifier) {
            check(dbName != Container.defaultDatabase) { "Tests cannot use the default database" }
            connection.prepareStatement("select 1 from pg_database where datname=?").use { s ->
                s.setString(1, dbName.toString())
                s.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        // no result row -> DB does not exist
                        connection.createStatement().use { it.executeUpdate("create database $dbName") }
                    }
                }
            }
        }

        override fun dropDatabaseIfExists(dbName: SafeIdentifier) {
            check(dbName != Container.defaultDatabase) { "Cannot drop the default database" }
            container().dataSource(Container.defaultDatabase.toString()).awaitConnectivity().use { connection ->
                connection.createStatement().use { s ->
                    s.executeUpdate("drop database if exists $dbName")
                }
            }
        }

        override fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource {
            return container().dataSource(databaseName.toString(), schemaName.toString())
        }

        override fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schemaName, Names.nextTable())
        }

        override fun close() = container.close()
    }
}
