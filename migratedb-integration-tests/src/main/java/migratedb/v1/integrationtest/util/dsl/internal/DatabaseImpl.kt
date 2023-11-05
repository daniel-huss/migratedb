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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.core.api.Checksum
import migratedb.v1.core.api.MigrationType
import migratedb.v1.core.api.Version
import migratedb.v1.core.api.configuration.Configuration
import migratedb.v1.core.api.configuration.FluentConfiguration
import migratedb.v1.core.api.internal.database.base.Database
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory
import migratedb.v1.core.internal.callback.NoopCallbackExecutor
import migratedb.v1.core.internal.jdbc.JdbcConnectionFactoryImpl
import migratedb.v1.core.internal.parser.ParsingContextImpl
import migratedb.v1.core.internal.resolver.MigrationInfoHelper
import migratedb.v1.core.internal.schemahistory.SchemaHistory
import migratedb.v1.core.internal.schemahistory.SchemaHistoryFactory
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.dsl.DatabaseSpec
import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.checksum
import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.toMigrationName
import migratedb.v1.integrationtest.util.dsl.SchemaHistoryEntry
import migratedb.v1.integrationtest.util.dsl.SchemaHistorySpec
import migratedb.v1.testing.util.base.Exec.tryAll
import java.sql.Connection
import javax.sql.DataSource

class DatabaseImpl(
    private val databaseHandle: DbSystem.Handle
) : DatabaseSpec, AutoCloseable {
    private var namespace: SafeIdentifier = Names.nextNamespace()
    private var schemaHistory: SchemaHistorySpecImpl? = null
    private var database: Database? = null
    private var schemaHistoryTable: String? = null
    private var initializer: ((Connection) -> Unit)? = null
    private var connectionFactoryToClose: JdbcConnectionFactoryImpl? = null

    override fun schemaHistory(table: String?, block: (SchemaHistorySpec).() -> Unit) {
        this.schemaHistoryTable = table
        this.schemaHistory = SchemaHistorySpecImpl().also(block)
    }

    override fun initializedBy(block: (Connection) -> Unit) {
        initializer = block
    }

    data class MaterializeResult(
        val namespace: SafeIdentifier,
        val adminDataSource: DataSource,
        val database: Database,
        val schemaName: SafeIdentifier?,
        val initializer: ((Connection) -> Unit)?
    )

    fun materialize(): MaterializeResult {
        val schemaName = databaseHandle.createNamespaceIfNotExists(namespace)?.let(databaseHandle::normalizeCase)
        val dataSource = databaseHandle.newAdminConnection(namespace)
        val configuration = FluentConfiguration().also {
            if (schemaName != null) it.schemas(schemaName.toString())
            if (schemaHistoryTable != null) it.table(schemaHistoryTable)
        }
        val db = JdbcConnectionFactoryImpl(dataSource::getConnection, configuration).let { connectionFactory ->
            connectionFactoryToClose = connectionFactory
            databaseHandle.type.createDatabase(configuration, connectionFactory).also {
                database = it
            }
        }

        schemaHistory?.materializeInto(db, configuration)
        return MaterializeResult(
            namespace = namespace,
            adminDataSource = dataSource,
            database = db,
            schemaName = schemaName,
            initializer = initializer
        )
    }

    override fun close() {
        tryAll(
            { database?.close() },
            { databaseHandle.dropNamespaceIfExists(namespace) },
            { connectionFactoryToClose?.close() }
        )
    }

    inner class SchemaHistorySpecImpl : SchemaHistorySpec {
        private val entries = mutableListOf<SchemaHistoryEntry>()

        override fun entry(
            version: Any?,
            description: String,
            type: MigrationType,
            success: Boolean,
            installedRank: Int?,
            checksum: Checksum?
        ) {
            entries.add(
                SchemaHistoryEntry(
                    type = type,
                    success = success,
                    version = when (version) {
                        null -> null
                        is Version -> version
                        else -> Version.parse(version.toString())
                    },
                    description = description,
                    checksum = checksum,
                    installedRank = installedRank,
                )
            )
        }

        override fun entry(name: String, type: MigrationType, success: Boolean, checksumDelta: Int) {
            val nameWithDescription = name.toMigrationName()
            val repeatable = nameWithDescription.startsWith("R")
            val prefix = nameWithDescription.take(1)
            val info = MigrationInfoHelper.extractVersionAndDescription(nameWithDescription, prefix, "__", repeatable)
            entry(
                version = info.version,
                description = info.description,
                type = type,
                success = success,
                checksum = nameWithDescription.checksum(checksumDelta)
            )
        }

        fun materializeInto(database: Database, configuration: Configuration) {
            val schemaHistory = getSchemaHistory(configuration, database)
            schemaHistory.create(false)
            entries.forEach { entry ->
                if (entry.installedRank != null) {
                    schemaHistory.addAppliedMigration(
                        entry.installedRank,
                        entry.version,
                        entry.description,
                        entry.type,
                        "n/a",
                        entry.checksum,
                        0,
                        entry.success
                    )
                } else {
                    schemaHistory.addAppliedMigration(
                        entry.version,
                        entry.description,
                        entry.type,
                        "n/a",
                        entry.checksum,
                        0,
                        entry.success
                    )
                }
            }
        }
    }


    companion object {
        /**
         * Creates a new schema history accessor.
         */
        fun getSchemaHistory(
            configuration: Configuration,
            database: Database
        ): SchemaHistory {
            val schema = SchemaHistoryFactory.scanSchemas(configuration, database).defaultSchema
            val sqlScriptExecutorFactory = database.databaseType.createSqlScriptExecutorFactory(
                DummyConnectionFactory,
                NoopCallbackExecutor.INSTANCE
            )
            val sqlScriptFactory = database.databaseType.createSqlScriptFactory(
                configuration,
                ParsingContextImpl()
            )
            return SchemaHistoryFactory.getSchemaHistory(
                configuration,
                sqlScriptExecutorFactory,
                sqlScriptFactory,
                database,
                schema
            )
        }
    }

    /**
     * Dummy object for use with SchemaHistoryFactory.getSchemaHistory, which requires a connection factory even though
     * its connections are never used...
     */
    private object DummyConnectionFactory : JdbcConnectionFactory {
        private fun wontImplement(): Nothing {
            throw UnsupportedOperationException("This is just a dummy")
        }

        override fun getDatabaseType() = wontImplement()

        override fun getJdbcUrl() = wontImplement()

        override fun getDriverInfo() = wontImplement()

        override fun getProductName() = wontImplement()

        override fun openConnection() = wontImplement()
    }
}
