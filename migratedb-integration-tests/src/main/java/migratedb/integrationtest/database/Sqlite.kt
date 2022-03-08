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
import migratedb.core.internal.database.sqlite.SQLiteDatabaseType
import migratedb.core.internal.jdbc.DriverDataSource
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.database.mutation.SqliteCreateTableMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.container.SharedResources
import migratedb.integrationtest.util.dependencies.DependencyResolver
import migratedb.integrationtest.util.dependencies.DependencyResolver.toClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

enum class Sqlite : DbSystem {
    V3_8_11_2,
    V3_14_2_1,
    V3_15_1,
    V3_16_1,
    V3_18_0,
    V3_19_3,
    V3_20_1,
    V3_21_0_1,
    V3_23_1,
    V3_25_2,
    V3_27_2_1,
    V3_28_0,
    V3_30_1,
    V3_31_1,
    V3_32_3_3,
    V3_34_0,
    V3_35_0_1,
    V3_36_0_3,
    ;

    companion object {
        private const val driverClass = "org.sqlite.JDBC"
        private val databaseType = SQLiteDatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }
        private val defaultSchema = "main".asSafeIdentifier()
        private val baseDir = Files.createTempDirectory(
            Paths.get("target", "sqlite").also { sqliteDir ->
                sqliteDir.createDirectories()
            },
            "it"
        ).also { tmpDir ->
            Runtime.getRuntime().addShutdownHook(Thread {
                tmpDir.toFile().deleteRecursively()
            })
            System.setProperty("org.sqlite.tmpdir ", tmpDir.toAbsolutePath().toString())
        }
    }

    private val driverCoordinates = "org.xerial:sqlite-jdbc:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        DependencyResolver.resolve(driverCoordinates).toClassLoader()
    }

    override fun toString() = "SQLite $name"

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle()
    }

    private inner class Handle : DbSystem.Handle {
        override val type: DatabaseType get() = Companion.databaseType

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            // A db exists as soon as we connect to it
            return defaultSchema
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            baseDir.resolve("$namespace.db").deleteIfExists()
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return DriverDataSource(
                classLoader,
                driverClass,
                "jdbc:sqlite:${baseDir.resolve("$namespace.db")}",
                "sa",
                "",
                databaseTypeRegister
            )
        }

        override fun nextMutation(namespace: SafeIdentifier): IndependentDatabaseMutation {
            return SqliteCreateTableMutation(Names.nextTable())
        }

        override fun close() {}
    }

}
