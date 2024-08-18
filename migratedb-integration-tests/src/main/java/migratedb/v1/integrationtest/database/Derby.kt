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

import migratedb.v1.core.api.internal.database.base.DatabaseType
import migratedb.v1.core.internal.database.derby.DerbyDatabaseType
import migratedb.v1.dependency_downloader.MavenCentralToLocal
import migratedb.v1.integrationtest.database.mutation.DerbyCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.container.Lease
import migratedb.v1.integrationtest.util.container.SharedResources
import org.apache.derby.client.BasicClientDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import javax.sql.DataSource


enum class Derby(baseImageTag: String) : DbSystem {
    V10_17_1_0("open-21-jre"),
    V10_16_1_1("open-17-jre"),
    V10_15_2_0("open-11-jre"),
    V10_14_2_0("open-11-jre"),
    V10_13_1_1("open-11-jre"),
    V10_12_1_1("open-8-jre")
    ;

    // Relevant idiosyncrasies:
    //  - Does not normalize database names because it's just a file name

    companion object {
        private val databaseType = DerbyDatabaseType()
        private const val SERVER_PORT = 1257
        private const val USER_AND_PASS = "sa"
    }

    private val containerAlias = "derby_${name.lowercase()}"
    private val derbyVersion = name.drop(1).replace('_', '.')
    private val serverCoordinates = "org.apache.derby:derbynet:$derbyVersion"
    private val serverJars = MavenCentralToLocal.resolver.resolve(listOf(serverCoordinates))

    private val image = run {
        val imageName = "migratedb-integration-test-apache-derby:$derbyVersion"
        ImageFromDockerfile(imageName, false)
            .apply {
                serverJars.asSequence()
                    .mapNotNull { it.artifact.file }
                    .forEach { serverJar ->
                        val fileNameDerbyServerExpects = serverJar.nameWithoutExtension.substringBefore("-") + ".jar"
                        withFileFromFile("/derby/$fileNameDerbyServerExpects", serverJar)
                    }
            }
            .withDockerfileFromBuilder { builder ->
                builder.from("ibm-semeru-runtimes:$baseImageTag")
                    .user("123456:123456")
                    .copy("/derby/*", "/opt/app/lib/")
                    .workDir("/tmp")
                    .entryPoint(
                        "java", "-cp", "/opt/app/lib/*",
                        "-Dderby.user.$USER_AND_PASS=$USER_AND_PASS",
                        "org.apache.derby.drda.NetworkServerControl", "start", "-p", "$SERVER_PORT", "-h", "0.0.0.0",
                    )
            }
    }

    inner class Container : GenericContainer<Container>(image) {
        init {
            withExposedPorts(SERVER_PORT)
            waitingFor(Wait.forListeningPort())
        }
    }

    override fun toString() = "Derby ${name.replace('_', '.')}"

    override fun get(sharedResources: SharedResources): DbSystem.Instance {
        return Instance(sharedResources.container(containerAlias, ::Container))
    }

    private inner class Instance(
        private val container: Lease<Container>
    ) : DbSystem.Instance, AutoCloseable by container {

        override val type: DatabaseType get() = Companion.databaseType

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            dataSource(namespace, create = true).work {
                it.execute("create schema $namespace")

                // The default schema is the same as the user name, and that schema does not exist by default.
                it.execute("create schema $USER_AND_PASS")
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            container().execInContainer("rm", "-rf", namespace.toString())
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return dataSource(namespace)
        }

        private fun dataSource(database: SafeIdentifier, create: Boolean = false): DataSource {
            val c = container()
            return BasicClientDataSource().also {
                it.databaseName = database.toString()
                it.user = USER_AND_PASS
                it.password = USER_AND_PASS
                it.serverName = c.host
                it.portNumber = c.getMappedPort(SERVER_PORT)
                it.createDatabase = if (create) "create" else null
            }
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return DerbyCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }
    }
}
