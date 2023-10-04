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
import migratedb.v1.core.internal.database.hsqldb.HSQLDBDatabaseType
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

enum class Hsqldb : DbSystem {
    V2_7_2,
    V2_6_1,
    V2_5_2,
    V2_4_1,
    V2_3_6,
    ;

    // Relevant idiosyncrasies:
    //  - None

    companion object {
        private const val driverClass = "org.hsqldb.jdbc.JDBCDriver"
        private val databaseType = HSQLDBDatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }

        init {
            // Check that HSQLDB is not on the test class path (because our custom class loader delegates to its parent)
            shouldThrow<ClassNotFoundException> {
                Class.forName(driverClass)
            }
        }
    }

    private val driverCoordinates = "org.hsqldb:hsqldb:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        MavenCentralToLocal.classLoaderFor(driverCoordinates)
    }

    override fun toString() = "HSQLDB ${name.replace('_', '.')}"

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
            var url = "jdbc:hsqldb:mem:$databaseName"
            schema?.let { url += ";schema=$it" }
            return SimpleDriverDataSource(
                ClassUtils.instantiate(driverClass, classLoader),
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
