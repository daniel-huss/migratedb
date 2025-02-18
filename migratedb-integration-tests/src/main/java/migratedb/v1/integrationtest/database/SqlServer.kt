/*
 * Copyright 2022-2024 The MigrateDB contributors
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

import com.microsoft.sqlserver.jdbc.SQLServerDataSource
import migratedb.v1.core.internal.database.sqlserver.SQLServerDatabaseType
import migratedb.v1.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class SqlServer(image: String) : DbSystem {
    V2022_CU5("mcr.microsoft.com/mssql/server:2022-CU16-ubuntu-22.04"),
    V2019_CU15("mcr.microsoft.com/mssql/server:2019-CU29-ubuntu-20.04"),
    V2017_CU28("mcr.microsoft.com/mssql/server:2017-CU28-ubuntu-16.04"),
    ;

    // Relevant idiosyncrasies:
    //  - Makes it really hard to change the "current" schema, so we just use databases for namespacing

    private val containerAlias = "sql_server_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "SQL Server ${name.replace('_', '.')}"

    companion object {
        private const val port = 1433
        private const val password = "AaaBbb0_"
        const val adminUser = "sa"
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String, database: String?): DataSource {
            return SQLServerDataSource().also {
                it.user = user
                it.setPassword(password)
                it.portNumber = getMappedPort(port)
                it.databaseName = database
                it.encrypt = false
            }
        }

        init {
            withEnv("ACCEPT_EULA", "Y")
            withEnv("SA_PASSWORD", password)
            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Instance {
        return Instance(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Instance(private val container: Lease<Container>) : DbSystem.Instance {
        override val type = SQLServerDatabaseType()

        private val internalDs by lazy { container().dataSource(adminUser, null) }

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            return internalDs.work {
                if (it.queryForObject("select DB_ID('$namespace')", Int::class.java) == null) {
                    it.update("create database $namespace")
                }
                it.queryForObject("select SCHEMA_NAME()", String::class.java)!!.asSafeIdentifier()
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(adminUser, "$namespace")
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            internalDs.work {
                it.update("drop database if exists $namespace")
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema, normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
