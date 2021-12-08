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

package migratedb.integrationtest.dsl

import migratedb.core.api.MigrationType
import migratedb.core.api.MigrationVersion
import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.internal.callback.NoopCallbackExecutor
import migratedb.core.internal.database.base.Database
import migratedb.core.internal.jdbc.JdbcConnectionFactory
import migratedb.core.internal.parser.ParsingContext
import migratedb.core.internal.resolver.MigrationInfoHelper
import migratedb.core.internal.schemahistory.SchemaHistoryFactory
import migratedb.integrationtest.Exec.tryAll
import migratedb.integrationtest.Names
import migratedb.integrationtest.NoOpIntercepter
import migratedb.integrationtest.SafeIdentifier
import migratedb.integrationtest.SafeIdentifier.Companion.requireSafeIdentifier
import migratedb.integrationtest.SharedResources
import migratedb.integrationtest.database.SupportedDatabase

class DatabaseSpec(
    private val sharedResources: SharedResources,
    private val supportedDatabase: SupportedDatabase
) : DslCallback {
    private var name: SafeIdentifier = Names.nextDatabase()
    private var schemaName: SafeIdentifier = Names.nextSchema()
    private var schemaHistory: SchemaHistorySpec? = null
    private var database: Database<*>? = null

    fun name(name: String) {
        this.name = name.requireSafeIdentifier()
    }

    fun schemaName(schemaName: String) {
        this.schemaName = schemaName.requireSafeIdentifier()
    }

    fun existingSchemaHistory(table: String, block: (SchemaHistorySpec).() -> Unit) {
        schemaHistory = SchemaHistorySpec(table).also(block)
    }

    override fun beforeWhen() {
        val dataSource = supportedDatabase.createDatabaseIfNotExists(sharedResources, name)
        val configuration = FluentConfiguration()
        val connectionFactory = JdbcConnectionFactory(dataSource, configuration, NoOpIntercepter)
        database = supportedDatabase.type.createDatabase(configuration, connectionFactory, NoOpIntercepter).also {
            schemaHistory?.materializeInto(it, connectionFactory)
        }
    }

    override fun cleanup() {
        tryAll(
            { database?.close() },
            { supportedDatabase.dropDatabaseIfExists(sharedResources, name) }
        )
    }


    private data class SchemaHistoryEntry(
        val type: MigrationType,
        val success: Boolean,
        val version: MigrationVersion,
        val description: String,
        val checksum: Int,
        val installedRank: Int?,
    )

    inner class SchemaHistorySpec(private val table: String) {
        private val entries = mutableListOf<SchemaHistoryEntry>()

        fun entry(name: String, type: MigrationType, success: Boolean, installedRank: Int? = null, checksum: Int = 0) {
            val repeatable = name.startsWith("R")
            val prefix = name.take(1)
            val info = MigrationInfoHelper.extractVersionAndDescription(name, prefix, "__", arrayOf(""), repeatable)
            entries.add(
                SchemaHistoryEntry(
                    type = type,
                    success = success,
                    version = info.left,
                    description = info.right,
                    checksum = checksum,
                    installedRank = installedRank,
                )
            )
        }

        fun materializeInto(database: Database<*>, connectionFactory: JdbcConnectionFactory) {
            val configuration = FluentConfiguration().schemas(schemaName.toString()).table(table)
            val sqlScriptExecutorFactory = database.databaseType.createSqlScriptExecutorFactory(
                connectionFactory,
                NoopCallbackExecutor.INSTANCE,
                NoOpIntercepter
            )
            val sqlScriptFactory = database.databaseType.createSqlScriptFactory(
                configuration,
                ParsingContext()
            )
            val schema = SchemaHistoryFactory.prepareSchemas(configuration, database).left

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
