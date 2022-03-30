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

import com.github.dockerjava.api.model.Capability
import com.ibm.db2.jcc.DB2SimpleDataSource
import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.api.internal.jdbc.StatementInterceptor
import migratedb.core.internal.database.db2.DB2DatabaseType
import migratedb.core.internal.jdbc.JdbcConnectionFactoryImpl
import migratedb.integrationtest.database.mutation.Db2CreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.awaitConnectivity
import migratedb.integrationtest.util.container.Lease
import migratedb.integrationtest.util.container.SharedResources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import javax.sql.DataSource

enum class Db2(image: String) : DbSystem {
    V11_5_7_0("ibmcom/db2:11.5.7.0"),
    ;

    // Relevant idiosyncracies:
    //  - When a table/column is created as "quoted_lower_case" it cannot be referenced using an unquoted identifier

    private val containerAlias = "db2_${name.lowercase()}"
    private val image = DockerImageName.parse(image)

    override fun toString() = "DB2 ${name.replace('_', '.')}"

    companion object {
        private const val password = "testtest1234"
        const val adminUser = "db2inst1"
        const val port = 50000
        const val defaultDatabase = "testdb"
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
            withEnv("DBNAME", defaultDatabase)
            withEnv("DB2INSTANCE", adminUser)
            withEnv("DB2INST1_PASSWORD", password)
            withEnv("LICENSE", "accept")
            withEnv("ARCHIVE_LOGS", "false")
            withEnv("AUTOCONFIG", "false")

            withCreateContainerCmdModifier {
                it.hostConfig!!.apply {
                    withCapAdd(Capability.IPC_LOCK, Capability.IPC_OWNER)
                }
            }

            withExposedPorts(port)
            withStartupTimeout(Duration.ofMinutes(5)) // DB2 startup is very slow
        }
    }

    private var handle: Handle? = null

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        // Intentionally never closing the obtained handle, because starting new DB2 containers takes so very long.
        synchronized(this) {
            var h = handle
            if (h == null) {
                h = Handle(sharedResources.container(containerAlias) { Container(image) })
                handle = h
            }
            return h
        }
    }

    private class Handle(private val container: Lease<Container>) : DbSystem.Handle {
        override val type = DB2DatabaseType()

        private val db = FluentConfiguration().let {
            val ds = container().dataSource()
            ds.awaitConnectivity(Duration.ofMinutes(5)).use { }
            val connectionFactory = JdbcConnectionFactoryImpl(ds, it, StatementInterceptor.doNothing())
            type.createDatabase(it, connectionFactory, StatementInterceptor.doNothing())
        }

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            val schema = db.mainConnection.getSchema(namespace.toString())
            if (!schema.exists()) {
                schema.create()
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            val schema = db.mainConnection.getSchema(namespace.toString())
            if (schema.exists()) {
                schema.drop()
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return container().dataSource(adminUser, namespace.toString())
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return Db2CreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() {
            container.use {
                db.use { }
            }
        }
    }
}
