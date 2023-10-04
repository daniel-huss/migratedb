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

import com.mysql.cj.jdbc.MysqlDataSource
import migratedb.v1.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import migratedb.v1.core.internal.database.mysql.MySQLDatabaseType
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class MySql(image: String) : DbSystem {
    V8_0("mysql:8.0"),
    V5_7("mysql:5.7"),
    ;

    // Relevant idiosyncrasies:
    //  - Doesn't use single quotes for string literals
    //  - Calls schemas "databases"
    //  - Doesn't normalize unquoted schema/database names

    private val containerAlias = "mysql_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "MySQL ${name.replace('_', '.')}"

    companion object {
        private const val port = 3306
        private const val password = "test"
        const val adminUser = "root"
        const val regularUser = "mysql"
        val defaultDatabase = "mysql".asSafeIdentifier()
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String, database: String): DataSource {
            return MysqlDataSource().also {
                it.user = user
                it.password = password
                it.port = getMappedPort(port)
                it.databaseName = database
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(300_000_000)
            }
            withEnv("MYSQL_USER", regularUser)
            withEnv("MYSQL_PASSWORD", password)
            withEnv("MYSQL_ROOT_PASSWORD", password)
            withEnv("MYSQL_DATABASE", defaultDatabase.toString())
            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = MySQLDatabaseType()

        private val internalDs = container().dataSource(adminUser, defaultDatabase.toString())

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            internalDs.work {
                it.update("create database if not exists ${normalizeCase(namespace)}")
            }
            return namespace
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(MariaDb.adminUser, normalizeCase(namespace).toString())
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            require(namespace != MariaDb.defaultDatabase) { "Cannot drop the default database" }
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
