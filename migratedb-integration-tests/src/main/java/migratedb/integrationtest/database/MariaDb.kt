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
import migratedb.integrationtest.SafeIdentifier
import migratedb.integrationtest.SharedResources
import migratedb.integrationtest.awaitConnectivity
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import javax.sql.DataSource

enum class MariaDb(image: String) : SupportedDatabase {
    V10_2("mariadb:10.2"),
    V10_3("mariadb:10.3"),
    V10_4("mariadb:10.4"),
    V10_5("mariadb:10.5"),
    V10_6("mariadb:10.6"),
    V10_7("mariadb:10.7"),
    ;

    private val image = DockerImageName.parse(image)
    override val type = MariaDBDatabaseType()

    override fun toString() = "MariaDB $name"

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        companion object {
            private const val port = 3306
            private const val password = "test"
            private const val user = "mariadb"
            private const val database = "mariadb"
        }

        fun dataSource(): DataSource {
            return MariaDbDataSource().also {
                it.user = user
                it.setPassword(password)
                it.port = port
                it.databaseName = database
            }
        }

        init {
            withEnv("MARIADB_USER", user)
            withEnv("MARIADB_PASSWORD", password)
            withEnv("MARIADB_ROOT_PASSWORD", password)
            withExposedPorts(port)
        }
    }

    private val containerAlias = "mariadb_${name.lowercase()}"

    override fun createDatabaseIfNotExists(sharedResources: SharedResources, dbName: SafeIdentifier): DataSource {
        return sharedResources.container(containerAlias) {
            Container(image)
        }.dataSource().also { ds ->
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

    override fun dropDatabaseIfExists(sharedResources: SharedResources, dbName: SafeIdentifier) {
        sharedResources.container<Container>(containerAlias)?.dataSource()?.awaitConnectivity()?.use { connection ->
            connection.createStatement().use { s ->
                s.executeUpdate("drop database if exists $dbName")
            }
        }
    }
}
