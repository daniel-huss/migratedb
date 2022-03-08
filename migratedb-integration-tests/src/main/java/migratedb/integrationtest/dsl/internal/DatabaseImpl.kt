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

package migratedb.integrationtest.dsl.internal

import migratedb.core.api.MigrationType
import migratedb.core.api.MigrationVersion
import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.api.internal.database.base.Database
import migratedb.core.api.internal.database.base.Schema
import migratedb.core.internal.callback.NoopCallbackExecutor
import migratedb.core.internal.jdbc.JdbcConnectionFactory
import migratedb.core.internal.parser.ParsingContext
import migratedb.core.internal.resolver.MigrationInfoHelper
import migratedb.core.internal.schemahistory.SchemaHistoryFactory
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.dsl.DatabaseSpec
import migratedb.integrationtest.dsl.SchemaHistorySpec
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.NoOpIntercepter
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier

class DatabaseImpl(
    private val databaseHandle: DbSystem.Handle
) : DatabaseSpec, AutoCloseable {
    private var namespace: SafeIdentifier = Names.nextNamespace()
    private var schemaHistory: SchemaHistorySpecImpl? = null
    private var database: Database<*>? = null

    override fun existingSchemaHistory(table: String, block: (SchemaHistorySpec).() -> Unit) {
        schemaHistory = SchemaHistorySpecImpl(table).also(block)
    }

    override fun tableName(s: CharSequence) = databaseHandle.normalizeCase(s)

    data class MaterializeResult(
        val namespace: SafeIdentifier,
        val database: Database<*>,
        val schemaName: SafeIdentifier,
    )

    fun materialize(): MaterializeResult {
        val schemaName = databaseHandle.normalizeCase(databaseHandle.createNamespaceIfNotExists(namespace))
        val dataSource = databaseHandle.newAdminConnection(namespace)
        val configuration = FluentConfiguration().schemas(schemaName)
        val connectionFactory = JdbcConnectionFactory(dataSource, configuration, NoOpIntercepter)
        val db = databaseHandle.type.createDatabase(configuration, connectionFactory, NoOpIntercepter)
        val schema = SchemaHistoryFactory.scanSchemas(configuration, db).defaultSchema
        schemaHistory?.materializeInto(db, schema, connectionFactory)
        database = db
        return MaterializeResult(namespace = namespace, database = db, schemaName = schemaName.asSafeIdentifier())
    }

    override fun close() {
        database.use { }
        databaseHandle.dropNamespaceIfExists(namespace)
    }

    private data class SchemaHistoryEntry(
        val type: MigrationType,
        val success: Boolean,
        val version: MigrationVersion?,
        val description: String,
        val checksum: Int,
        val installedRank: Int?,
    )

    inner class SchemaHistorySpecImpl(private val table: String) : SchemaHistorySpec {
        private val entries = mutableListOf<SchemaHistoryEntry>()

        override fun entry(name: String, type: MigrationType, success: Boolean, installedRank: Int?, checksum: Int) {
            val repeatable = name.startsWith("R")
            val prefix = name.take(1)
            val info = MigrationInfoHelper.extractVersionAndDescription(name, prefix, "__", repeatable)
            entries.add(
                SchemaHistoryEntry(
                    type = type,
                    success = success,
                    version = info.version,
                    description = info.description,
                    checksum = checksum,
                    installedRank = installedRank,
                )
            )
        }

        fun materializeInto(database: Database<*>, schema: Schema<*, *>, connectionFactory: JdbcConnectionFactory) {
            val configuration = FluentConfiguration().schemas(schema.name).table(table)
            val sqlScriptExecutorFactory = database.databaseType.createSqlScriptExecutorFactory(
                connectionFactory,
                NoopCallbackExecutor.INSTANCE,
                NoOpIntercepter
            )
            val sqlScriptFactory = database.databaseType.createSqlScriptFactory(
                configuration,
                ParsingContext()
            )
            val schemaHistory = SchemaHistoryFactory.getSchemaHistory(
                configuration,
                sqlScriptExecutorFactory,
                sqlScriptFactory,
                database,
                schema,
                NoOpIntercepter
            )

            schemaHistory.create(false)
            entries.forEach {
                if (it.installedRank != null) {
                    schemaHistory.addAppliedMigration(
                        it.installedRank,
                        it.version,
                        it.description,
                        it.type,
                        "",
                        it.checksum,
                        0,
                        it.success
                    )
                } else {
                    schemaHistory.addAppliedMigration(
                        it.version,
                        it.description,
                        it.type,
                        "",
                        it.checksum,
                        0,
                        it.success
                    )
                }
            }
        }
    }
}
