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

import io.kotest.assertions.throwables.shouldThrow
import migratedb.v1.core.api.internal.database.base.DatabaseType
import migratedb.v1.core.internal.database.DatabaseTypeRegisterImpl
import migratedb.v1.core.internal.database.h2.H2DatabaseType
import migratedb.v1.core.internal.util.ClassUtils
import migratedb.v1.dependency_downloader.MavenCentralToLocal
import migratedb.v1.integrationtest.database.mutation.BasicCreateTableMutation
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.container.SharedResources
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import javax.sql.DataSource

enum class H2 : DbSystem {
    V2_2_224,
    V2_1_210,
    V1_4_200,
    ;

    // Relevant idiosyncrasies:
    //  - None

    companion object {
        private const val DRIVER_CLASS = "org.h2.Driver"

        private val databaseType = H2DatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }

        init {
            // Check that H2 is not on the test class path (because our custom class loader delegates to its parent)
            shouldThrow<ClassNotFoundException> {
                Class.forName(DRIVER_CLASS)
            }
        }
    }

    private val driverCoordinates = "com.h2database:h2:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        MavenCentralToLocal.classLoaderFor(driverCoordinates)
    }

    override fun toString() = "H2 ${name.replace('_', '.')}"

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle()
    }

    private inner class Handle : DbSystem.Handle {
        override val type: DatabaseType get() = Companion.databaseType
        private val databaseName = Names.nextFile()

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            dataSource().work {
                it.execute("create schema $namespace")
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            dataSource().work {
                it.execute("drop schema $namespace cascade")
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return dataSource(namespace)
        }

        private fun dataSource(schema: SafeIdentifier? = null): DataSource {
            var url = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
            schema?.let { url += ";SCHEMA=$it" }
            return SimpleDriverDataSource(
                ClassUtils.instantiate(DRIVER_CLASS, classLoader),
                url,
                "sa",
                ""
            )
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return BasicCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() {
            dataSource().connection.use { connection ->
                connection.createStatement().use { it.execute("shutdown") }
            }
        }
    }
}
