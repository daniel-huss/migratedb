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
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.base.awaitConnectivity
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import javax.sql.DataSource

enum class Postgres(image: String) : DatabaseSupport {
    V9_6("postgres:9.6-alpine"),
    V10("postgres:10-alpine"),
    V11("postgres:11-alpine"),
    V12("postgres:12-alpine"),
    V13("postgres:13-alpine"),
    V14("postgres:14-alpine"),
    ;

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
        private val internalDs = container().dataSource(Container.defaultDatabase.toString())

        override fun createDatabaseIfNotExists(databaseName: SafeIdentifier): DataSource {
            check(databaseName != Container.defaultDatabase) { "Tests cannot use the default database" }
            internalDs.awaitConnectivity().use {
                createDatabaseIfNotExists(it, databaseName)
            }
            return container().dataSource(databaseName.toString())
        }

        private fun createDatabaseIfNotExists(connection: Connection, databaseName: SafeIdentifier) {
            connection.work {
                val rows = it.queryForList("select 1 from pg_database where datname='$databaseName'")
                if (rows.isEmpty()) {
                    it.update("create database $databaseName")
                }
            }
        }

        override fun dropDatabaseIfExists(databaseName: SafeIdentifier) {
            require(databaseName != Container.defaultDatabase) { "Cannot drop the default database" }
            internalDs.awaitConnectivity().use { connection ->
                connection.work {
                    it.update("drop database if exists $databaseName")
                }
            }
        }

        override fun createSchemaIfNotExists(databaseName: SafeIdentifier, schemaName: SafeIdentifier): SafeIdentifier {
            return schemaName.also {
                container().dataSource(databaseName.toString()).awaitConnectivity().use { connection ->
                    connection.work {
                        it.update("create schema if not exists $schemaName")
                    }
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
