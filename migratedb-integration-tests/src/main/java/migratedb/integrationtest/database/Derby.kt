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

import migratedb.core.api.internal.database.base.DatabaseType
import migratedb.core.internal.database.DatabaseTypeRegisterImpl
import migratedb.core.internal.database.derby.DerbyDatabaseType
import migratedb.core.internal.jdbc.DriverDataSource
import migratedb.integrationtest.database.mutation.DerbyCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.createDatabaseTempDir
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.SharedResources
import migratedb.integrationtest.util.dependencies.DependencyResolver
import migratedb.integrationtest.util.dependencies.DependencyResolver.toClassLoader
import java.nio.file.Path
import java.sql.Connection
import java.sql.Driver
import java.util.*
import javax.sql.DataSource
import kotlin.io.path.exists

enum class Derby : DbSystem {
    V10_15_2_0,
    V10_14_2_0,
    V10_13_1_1,
    V10_12_1_1,
    ;

    // Relevant idiosyncracies:
    //  - Does not normalize database names because it's just a file name

    companion object {
        private val databaseType = DerbyDatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }

        init {
            System.setProperty("derby.stream.error.file", createDatabaseTempDir("derby").resolve("derby.log").toString())
            System.setProperty("derby.infolog.append", "true")
        }
    }

    private val driverCoordinates = "org.apache.derby:derby:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        DependencyResolver.resolve(driverCoordinates).toClassLoader()
    }
    private val driverClass: String by lazy {
        ServiceLoader.load(Driver::class.java, classLoader)
            .stream()
            .filter { it.type().name.startsWith("org.apache.derby") }
            .findFirst().orElseThrow()
            .type().name
    }

    override fun toString() = "Derby ${name.replace('_', '.')}"

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle()
    }

    private inner class Handle : DbSystem.Handle {
        override val type: DatabaseType get() = Companion.databaseType
        private val dataDir = createDatabaseTempDir("derby-$name", deleteOnExit = false)

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            val dbPath = dbPath(namespace)
            if (!dbPath.exists()) {
                dataSource(dbPath, create = true).work {
                    it.execute("create schema $namespace")
                }
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            val dbPath = dbPath(namespace)
            if (dbPath.exists()) {
                dbPath.toFile().deleteRecursively()
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return dataSource(dbPath(namespace))
        }

        private fun dataSource(dbPath: Path, create: Boolean = false): DataSource {
            val url = "jdbc:derby:${dbPath.toAbsolutePath()}"
            return object : DriverDataSource(
                classLoader,
                driverClass,
                url,
                "",
                "",
                mapOf("create" to "$create"),
                databaseTypeRegister
            ) {
                // There seems to be some sort of concurrency issue with this driver, so we synchronize getConnection()
                override fun getConnection(): Connection {
                    return synchronized(Derby::class) {
                        super.getConnection()
                    }
                }
            }
        }

        private fun dbPath(namespace: SafeIdentifier): Path {
            return dataDir.resolve(normalizeCase(namespace).toString()).toAbsolutePath()
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return DerbyCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() {
            dataDir.toFile().deleteRecursively()
        }
    }
}
