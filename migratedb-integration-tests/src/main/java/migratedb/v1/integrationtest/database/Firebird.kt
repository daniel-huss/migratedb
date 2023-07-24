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

package migratedb.v1.integrationtest.database

import migratedb.v1.integrationtest.database.mutation.FirebirdCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import migratedb.v1.core.internal.database.firebird.FirebirdDatabaseType
import org.firebirdsql.ds.FBSimpleDataSource
import org.firebirdsql.management.FBMaintenanceManager
import org.firebirdsql.management.FBManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

enum class Firebird(image: String) : DbSystem {
    V3_0_9("jacobalberty/firebird:v3.0.9"),
    V4_0_1("jacobalberty/firebird:v4.0.1"),
    ;

    // Relevant idiosyncrasies:
    //  - Doesn't support schemas
    //  - Doesn't allow qualified table names
    //  - Doesn't allow CREATE DATABASE via JDBC statement


    private val containerAlias = "firebird_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "Firebird ${name.replace('_', '.')}"

    companion object {
        private const val port = 3050
        private const val password = "insecure"
        private const val adminUser = "sysdba"
        private const val defaultUser = "fbuser"
        private const val defaultDatabase = "testdb"

        init {
            System.setProperty("org.firebirdsql.jdbc.disableLogging", "true")
        }
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String = adminUser, database: String = defaultDatabase): DataSource {
            return FBSimpleDataSource().also {
                it.charSet = "UTF-8"
                it.userName = user
                it.password = password
                it.database = "//$host:${getMappedPort(port)}/$database"
            }
        }

        fun withFbManager(block: (FBManager) -> Unit) {
            return FBManager().let {
                it.server = host
                it.port = getMappedPort(port)
                it.start()
                try {
                    block(it)
                } finally {
                    it.stop()
                }
            }
        }

        fun withFbMaintenanceManager(database: String, block: (FBMaintenanceManager) -> Unit) {
            return FBMaintenanceManager().let {
                it.host = host
                it.port = getMappedPort(port)
                it.database = database
                it.user = adminUser
                it.password = password
                block(it)
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(300_000_000)
            }
            withEnv("FIREBIRD_DATABASE", defaultDatabase)
            withEnv("FIREBIRD_USER", defaultUser)
            withEnv("FIREBIRD_PASSWORD", password)
            withEnv("ISC_PASSWORD", password)
            withExposedPorts(port)
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = FirebirdDatabaseType()

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier? {
            container().withFbManager {
                it.createDatabase(namespace.toString(), adminUser, password)
            }
            return null
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(adminUser, "$namespace")
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            container().withFbManager { manager ->
                if (manager.isDatabaseExists(namespace.toString(), adminUser, password)) {
                    manager.dropDatabase(namespace.toString(), adminUser, password)
                }
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return FirebirdCreateTableMutation(normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
