/*
 * Copyright 2022-2024 The MigrateDB contributors
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
import migratedb.v1.core.internal.database.sqlite.SQLiteDatabaseType
import migratedb.v1.core.internal.util.ClassUtils
import migratedb.v1.dependency_downloader.MavenCentralToLocal
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.database.mutation.SqliteCreateTableMutation
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.v1.integrationtest.util.container.SharedResources
import migratedb.v1.testing.util.io.newTempDir
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.nio.file.Path
import javax.sql.DataSource
import kotlin.io.path.deleteIfExists

enum class Sqlite : DbSystem {
    V3_46_0_1,
    V3_36_0_3,
    V3_35_0_1,
    V3_34_0,
    V3_32_3_3,
    V3_31_1,
    V3_30_1,
    V3_28_0,
    V3_27_2_1,
    V3_25_2,
    V3_23_1,
    V3_21_0_1,
    V3_20_1,
    V3_19_3,
    V3_18_0,
    V3_16_1,
    V3_15_1,
    V3_14_2_1,
    V3_8_11_2,
    ;

    // Relevant idiosyncrasies:
    //  - Doesn't really support schemas, although other databases can be "attached" with a custom alias to qualify
    //    its tables (we don't use that feature here)

    companion object {
        private const val driverClass = "org.sqlite.JDBC"
        private val databaseType = SQLiteDatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }
        private val defaultSchema = "main".asSafeIdentifier()

        init {
            // Check that SQLite is not on the test class path (because our custom class loader delegates to its parent)
            shouldThrow<ClassNotFoundException> {
                Class.forName(driverClass)
            }
        }
    }

    private val driverCoordinates = "org.xerial:sqlite-jdbc:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy(Sqlite::class) {
        MavenCentralToLocal.classLoaderFor(driverCoordinates).also {
            synchronized(Sqlite::class.java) {
                // This JDBC driver extracts native libraries (ugh!) to the directory specified by the system property
                // "org.sqlite.tmpdir". Since we want to use multiple versions of the library, each one has to extract its
                // shit into a different directory. This all assumes that the corresponding system property is read exactly
                // once, during initialization. This seems to be the case, so this hack may actually work.
                System.setProperty("org.sqlite.tmpdir", newTempDir("sqlite-driver-$name").toString())
                val loader = it.loadClass("org.sqlite.SQLiteJDBCLoader")
                loader.getMethod("initialize").invoke(null)
                // Initialization should not ever happen outside this code block, so:
                System.setProperty("org.sqlite.tmpdir", "/dev/null")
            }
        }
    }

    override fun toString() = "SQLite ${name.replace('_', '.')}"

    override fun get(sharedResources: SharedResources): DbSystem.Instance {
        return Instance()
    }

    private inner class Instance : DbSystem.Instance {
        override val type: DatabaseType get() = Companion.databaseType
        private val dataDir by lazy { newTempDir("sqlite-$name", deleteOnExit = false) }

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            // A db exists as soon as we connect to it
            return defaultSchema
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            databaseFile(namespace).deleteIfExists()
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return SimpleDriverDataSource(
                ClassUtils.instantiate(driverClass, classLoader),
                "jdbc:sqlite:${databaseFile(namespace)}",
                "sa",
                ""
            )
        }

        private fun databaseFile(namespace: SafeIdentifier): Path {
            return dataDir.resolve("$namespace.db")
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return SqliteCreateTableMutation(normalizeCase(Names.nextTable()))
        }

        override fun close() {
            dataDir.toFile().deleteRecursively()
        }
    }
}
