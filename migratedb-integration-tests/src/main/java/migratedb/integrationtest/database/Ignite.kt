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

import de.unentscheidbar.ignite.jdbc.IgniteJdbcThinDataSource
import migratedb.core.internal.database.oracle.OracleDatabaseType
import migratedb.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import javax.sql.DataSource

enum class Ignite(image: String) : DbSystem {
    V2_12_0("apacheignite/ignite:2.12.0"),
    ;

    // Relevant idiosyncracies:
    //  - ???

    private val containerAlias = "ignite_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "Ignite ${name.replace('_', '.')}"

    companion object {
        private const val port = 10800
        private const val adminUser = "sa"
        private const val password = ""
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String = adminUser): DataSource {
            return IgniteJdbcThinDataSource().also {
                it.username = user
                it.password = password
                it.setAddresses("$host:${getMappedPort(port)}")
            }
        }

        init {
            withExposedPorts(port)
        }
    }

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle(sharedResources.container(containerAlias) { Container(image) })
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = OracleDatabaseType()

        private val internalDs = container().dataSource()

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            internalDs.work(timeout = Duration.ofMinutes(1)) {
                it.execute("create schema $namespace")
            }
            return namespace
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(user = namespace.toString())
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            internalDs.work {
                it.update("drop schema $namespace cascade")
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
