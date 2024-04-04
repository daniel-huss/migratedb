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

import com.github.dockerjava.api.model.Capability
import com.ibm.db2.jcc.DB2SimpleDataSource
import migratedb.v1.core.api.configuration.FluentConfiguration
import migratedb.v1.core.internal.database.db2.DB2Database
import migratedb.v1.core.internal.database.db2.DB2DatabaseType
import migratedb.v1.core.internal.database.db2.DB2Schema
import migratedb.v1.core.internal.jdbc.JdbcConnectionFactoryImpl
import migratedb.v1.integrationtest.database.mutation.Db2CreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.awaitConnectivity
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import javax.sql.DataSource

enum class Db2(image: String) : DbSystem {
    V11_5_9_0("icr.io/db2_community/db2:11.5.9.0"),
    ;

    // Relevant idiosyncrasies:
    //  - When a table/column is created as "quoted_lower_case" it cannot be referenced using an unquoted identifier

    private val containerAlias = "db2_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "DB2 ${name.replace('_', '.')}"

    companion object {
        private const val password = "testtest1234"
        private const val adminUser = "db2inst1"
        private const val port = 50000
        private const val defaultDatabase = "testdb"
    }

    inner class Container(image: DockerImageName) : GenericContainer<Container>(image) {
        fun dataSource(user: String = adminUser, currentSchema: String? = null): DataSource {
            return DB2SimpleDataSource().also {
                it.driverType = 4
                it.serverName = host
                it.user = user
                it.setPassword(password)
                it.portNumber = getMappedPort(port)
                it.databaseName = defaultDatabase
                it.currentSchema = currentSchema
            }
        }

        init {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(500_000_000)
            }
            withEnv("DBNAME", defaultDatabase)
            withEnv("DB2INSTANCE", adminUser)
            withEnv("DB2INST1_PASSWORD", password)
            withEnv("LICENSE", "accept")
            withEnv("ARCHIVE_LOGS", "false")
            withEnv("AUTOCONFIG", "false")
            withEnv("BLU", "false")
            withEnv("ENABLE_ORACLE_COMPATIBILITY", "false")
            withEnv("UPDATEAVAIL", "NO")
            withEnv("TO_CREATE_SAMPLEDB", "false")
            withEnv("REPODB", "false")
            withEnv("IS_OSXFS", "false")
            withEnv("PERSISTENT_HOME", "true")
            withEnv("HADR_ENABLED", "false")

            withCreateContainerCmdModifier {
                it.hostConfig!!.apply {
                    withCapAdd(Capability.IPC_LOCK, Capability.IPC_OWNER)
                }
            }

            withExposedPorts(port)
            waitingFor(Wait.forListeningPort())
            withStartupTimeout(Duration.ofMinutes(10)) // DB2 startup is very, very, very slow
        }
    }

    /**
     * The container is so slow to start that we only ever start one, holding onto the lease forever.
     */
    private var neverClosedContainerLease: Lease<Container>? = null

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        synchronized(this) {
            var container = neverClosedContainerLease
            if (container == null) {
                container = sharedResources.container(containerAlias) { Container(image) }
                neverClosedContainerLease = container
            }
            return Handle(container)
        }
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = DB2DatabaseType()

        private val configStub = FluentConfiguration()

        private val connectionFactory: JdbcConnectionFactoryImpl by lazy {
            val ds = container().dataSource()
            ds.awaitConnectivity(Duration.ofMinutes(5)).use { }
            JdbcConnectionFactoryImpl(ds::getConnection, configStub)
        }

        private val db by lazy { type.createDatabase(configStub, connectionFactory) as DB2Database }

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            val schema = db.mainSession.getSchema(namespace.toString())
            if (!schema.exists()) {
                schema.create()
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            Db2SchemaDropper(
                db.mainSession.getSchema(namespace.toString()) as DB2Schema,
                db,
                db.mainSession.jdbcTemplate
            ).drop()
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(adminUser, namespace.toString())
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return Db2CreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() {
            // Do NOT close the container lease!
            db.use {
                connectionFactory.use { }
            }
        }
    }
}
