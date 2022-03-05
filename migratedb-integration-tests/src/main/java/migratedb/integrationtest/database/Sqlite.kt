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
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.SharedResources
import migratedb.integrationtest.util.dependencies.DependencyResolver
import migratedb.integrationtest.util.dependencies.DependencyResolver.toClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import javax.sql.DataSource
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

enum class Sqlite : DatabaseSupport {
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
            "d"
        ).also { tmpDir ->
            Runtime.getRuntime().addShutdownHook(Thread {
                tmpDir.toFile().deleteRecursively()
            })
            System.setProperty("org.sqlite.tmpdir ", tmpDir.toString())
        }
    }

    private val driverCoordinates = "org.xerial:sqlite-jdbc:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        DependencyResolver.resolve(driverCoordinates).toClassLoader()
    }

    override fun toString() = "SQLite $name"

    override fun get(sharedResources: SharedResources): DatabaseSupport.Handle {
        return Handle()
    }

    private inner class Handle : DatabaseSupport.Handle {
        override val type: DatabaseType get() = Companion.databaseType

        override fun createDatabaseIfNotExists(databaseName: SafeIdentifier): DataSource {
            // A db exists as soon as we connect to it
            return newAdminConnection(databaseName, defaultSchema)
        }

        override fun dropDatabaseIfExists(databaseName: SafeIdentifier) {
            baseDir.resolve("$databaseName.db").deleteIfExists()
        }

        override fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource {
            requireDefaultSchema(schemaName)
            return DriverDataSource(
                classLoader,
                driverClass,
                "jdbc:sqlite:${baseDir.resolve("$databaseName.db")}",
                "sa",
                "",
                databaseTypeRegister
            )
        }

        override fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation {
            requireDefaultSchema(schemaName)
            return CreateTableMutation(Names.nextTable())
        }

        private fun requireDefaultSchema(schemaName: SafeIdentifier) {
            require(schemaName == defaultSchema) { "SQLite does not support schemas" }
        }

        override fun createSchemaIfNotExists(databaseName: SafeIdentifier, schemaName: SafeIdentifier): SafeIdentifier? = null

        override fun close() {}
    }


    /**
     * Creates / drops a table whose name is not shared with other instances of this mutation.
     */
    private class CreateTableMutation(private val tableName: SafeIdentifier) :
        IndependentDatabaseMutation() {

        override fun isApplied(connection: Connection): Boolean {
            return connection.work {
                it.query("select name from sqlite_master where type='table' and name='$tableName'") { _, _ ->
                    true
                }.isNotEmpty()
            }
        }

        override fun apply(connection: Connection) {
            connection.work {
                it.execute("create table $tableName(id int not null primary key)")
            }
        }

        override fun undo(connection: Connection) {
            connection.work {
                it.execute("drop table $tableName")
            }
        }
    }

}
