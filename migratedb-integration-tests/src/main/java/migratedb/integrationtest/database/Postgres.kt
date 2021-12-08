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
import migratedb.integrationtest.SafeIdentifier
import migratedb.integrationtest.SafeIdentifier.Companion.requireSafeIdentifier
import migratedb.integrationtest.SharedResources
import migratedb.integrationtest.awaitConnectivity
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import javax.sql.DataSource

enum class Postgres(image: String) : SupportedDatabase {
    V9_6("postgres:9.6-alpine"),
    V10("postgres:10-alpine"),
    V11("postgres:11-alpine"),
    V12("postgres:12-alpine"),
    V13("postgres:13-alpine"),
    ;

    private val image = DockerImageName.parse(image)

    override fun toString() = "PostgreSQL $name"

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        companion object {
            const val port = 5432
            const val password = "test"
            const val user = "postgres"
            val defaultDatabase = "postgres".requireSafeIdentifier()
        }

        fun dataSource(databaseName: String): DataSource {
            return PGSimpleDataSource().also {
                it.user = user
                it.password = password
                it.portNumbers = intArrayOf(getMappedPort(port))
                it.applicationName = "MigrateDB Integration Test"
                it.loggerLevel = "OFF"
                it.serverNames = arrayOf(host)
                it.databaseName = databaseName
            }
        }

        init {
            withEnv("POSTGRES_PASSWORD", password)
            withCreateContainerCmdModifier {
                it.withCmd(
                    "-c", "fsync=off",
                    "-c", "log_destination=stderr",
                    "-c", "log_statement=all"
                )
            }
            withExposedPorts(port)
        }
    }

    private val containerAlias = "postgres_${name.lowercase()}"
    override val type: DatabaseType =
        PostgreSQLDatabaseType()

    override fun createDatabaseIfNotExists(sharedResources: SharedResources, dbName: SafeIdentifier): DataSource {
        check(dbName != Container.defaultDatabase) { "Tests cannot use the default database " }
        return sharedResources.container(containerAlias) {
            Container(image)
        }.dataSource(Container.defaultDatabase.toString()).also { ds ->
            ds.awaitConnectivity().use {
                createDatabaseIfNotExists(it, dbName)
            }
        }
    }

    private fun createDatabaseIfNotExists(connection: Connection, dbName: SafeIdentifier) {
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

    override fun dropDatabaseIfExists(sharedResources: SharedResources, dbName: SafeIdentifier) {
        check(dbName != Container.defaultDatabase) { "Cannot drop the default database" }
        sharedResources.container<Container>(containerAlias)
            ?.dataSource(Container.defaultDatabase.toString())
            ?.awaitConnectivity()?.use { connection ->
                connection.createStatement().use { s ->
                    s.executeUpdate("drop database if exists $dbName")
                }
            }
    }
}
