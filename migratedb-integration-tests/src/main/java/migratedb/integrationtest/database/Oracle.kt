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

import migratedb.core.internal.database.oracle.OracleDatabaseType
import migratedb.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.rethrowUnless
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import oracle.jdbc.pool.OracleDataSource
import org.springframework.dao.DataAccessException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import javax.sql.DataSource

enum class Oracle(image: String) : DbSystem {
    V18_4_0("gvenzl/oracle-xe:18.4.0-slim"),
    V21_3_0("gvenzl/oracle-xe:21.3.0-slim"),
    ;

    // Relevant idiosyncracies:
    //  - Treats the empty string as NULL
    //  - Schemas and users are kinda the same thing
    //  - Passwords must be valid table/column identifiers

    private val containerAlias = "oracle_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "Oracle ${name.replace('_', '.')}"

    companion object {
        private const val port = 1521
        private const val adminUser = "system"
        private const val password = "insecure"
    }

    class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String = adminUser): DataSource {
            return OracleDataSource().also {
                it.user = user
                it.setPassword(password)
                it.url = "jdbc:oracle:thin:@$host:${getMappedPort(port)}/XEPDB1"
            }
        }

        init {
            withEnv("ORACLE_PASSWORD", password)
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
                try {
                    it.execute("create user $namespace identified by $password")
                    it.execute("grant all privileges to $namespace")
                } catch (e: DataAccessException) {
                    e.rethrowUnless { sqlException -> sqlException.errorCode == 1920 }
                }
            }
            return namespace
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(user = namespace.toString())
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            internalDs.work {
                try {
                    it.update("drop user $namespace cascade")
                } catch (e: DataAccessException) {
                    e.rethrowUnless { sqlException -> sqlException.errorCode == 1918 }
                }
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() = container.close()
    }
}
