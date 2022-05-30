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

import com.informix.jdbcx.IfxDataSource
import migratedb.core.internal.database.informix.InformixDatabaseType
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.database.mutation.InformixCreateTableMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import javax.sql.DataSource

enum class Informix(image: String) : DbSystem {
    V14_10("ibmcom/informix-developer-database:14.10.FC7W1DE"),
    V12_10("ibmcom/informix-developer-database:12.10.FC11DE"),
    ;

    // Relevant idiosyncracies:
    //  - Does not support schemas as namespaces (CREATE SCHEMA just assigns the same owner to multiple DB objects)

    private val containerAlias = "informix_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "Informix ${name.replace('_', '.')}"

    companion object {
        private const val port = 9088
        private const val adminUser = "informix"
        private const val password = "in4mix"
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(database: String? = null): DataSource {
            return IfxDataSource().also {
                it.user = adminUser
                it.password = password
                it.ifxIFXHOST = host
                it.portNumber = getMappedPort(port)
                if (database != null) {
                    it.databaseName = database
                }
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(300_000_000)
            }
            withEnv("LICENSE", "accept")
            withEnv("STORAGE", "local")
            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = InformixDatabaseType()

        private val internalDs = container().dataSource()

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier? {
            internalDs.work(connectTimeout = Duration.ofMinutes(1)) {
                it.execute("create database if not exists $namespace with buffered log")
            }
            return null
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(namespace.toString())
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            internalDs.work {
                it.execute("drop database if exists $namespace")
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            check(schema == null) { "Informix does not support schemas" }
            return InformixCreateTableMutation(normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
