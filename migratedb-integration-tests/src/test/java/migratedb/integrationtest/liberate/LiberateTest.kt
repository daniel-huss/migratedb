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

package migratedb.integrationtest.liberate

import io.kotest.inspectors.shouldForExactly
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import migratedb.core.api.MigrationType
import migratedb.core.api.Version
import migratedb.integrationtest.database.*
import migratedb.integrationtest.util.base.IntegrationTest
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.dsl.Dsl
import migratedb.testing.util.base.Args
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.callback.NoopCallbackExecutor
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory
import org.flywaydb.core.internal.logging.multi.MultiLogger
import org.flywaydb.core.internal.parser.ParsingContext
import org.flywaydb.core.internal.schemahistory.SchemaHistoryFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.random.Random
import org.flywaydb.core.api.MigrationType as FwMigrationType
import org.flywaydb.core.api.MigrationVersion as FwMigrationVersion
import org.flywaydb.core.api.configuration.FluentConfiguration as FwConfiguration
import org.flywaydb.core.internal.schemahistory.AppliedMigration as FwAppliedMigration

class LiberateTest : IntegrationTest() {

    @ParameterizedTest
    @ArgumentsSource(DatabasesSupportedByFw::class)
    fun `Undo migrations are handled by removing both migrations`(dbSystem: DbSystem) = withDsl(dbSystem) {
        val oldSchemaHistoryTable = Names.nextTable().toString()
        given {
            database { }
            fwSchemaHistory(oldSchemaHistoryTable) {
                entry(version = "1", description = "Foo", type = "SQL", success = true)         // Should be skipped
                entry(version = "1", description = "Foo", type = "UNDO_JDBC", success = true)   // Should be skipped
                entry(version = "2", description = "Bar", type = "JDBC", success = true)        // Should be copied
                entry(version = "2", description = "Bar", type = "UNDO_SQL", success = false)   // Should be skipped
            }
        }.`when` {
            liberate {
                withConfig {
                    oldTable(oldSchemaHistoryTable)
                }
            }
        }.then { actual ->
            actual.oldSchemaHistoryTable.shouldBeEqualIgnoringCase(oldSchemaHistoryTable)
            actual.actions.shouldForExactly(2) {
                it.type.shouldBe("skipped_undo_migration")
            }
            actual.actions.shouldForOne {
                it.type.shouldBe("skipped_undone_migration")
            }
            actual.actions.shouldForOne {
                it.type.shouldBe("copied_jdbc_migration")
            }
            schemaHistory {
                filter { it.version == Version.parse("2") }.shouldBeSingleton {
                    it.description.shouldBe("Bar")
                    it.type.shouldBe(MigrationType.JDBC)
                    it.isSuccess.shouldBe(true)
                }
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DatabasesSupportedByFw::class)
    fun `Deletion markers are handled by removing the migration and skipping the marker`(dbSystem: DbSystem) =
        withDsl(dbSystem) {
            val oldSchemaHistoryTable = Names.nextTable().toString()
            given {
                database { }
                fwSchemaHistory(oldSchemaHistoryTable) {
                    entry(version = "1", description = "Foo", type = "SQL", success = true)
                    entry(version = null, description = "RepeatMe", type = "JDBC", success = false)
                    entry(version = "2", description = "NotDeleted", type = "SQL", success = true)
                    entry(version = "1", description = "Foo", type = "DELETE", success = true)
                    entry(version = null, description = "RepeatMe", type = "DELETE", success = true)
                }
            }.`when` {
                liberate {
                    withConfig {
                        oldTable(oldSchemaHistoryTable)
                    }
                }
            }.then { actual ->
                actual.oldSchemaHistoryTable.shouldBeEqualIgnoringCase(oldSchemaHistoryTable)
                actual.actions.shouldForExactly(2) {
                    it.type.shouldBe("skipped_deleted_migration")
                }
                actual.actions.shouldForOne {
                    it.type.shouldBe("copied_sql_migration")
                }
                schemaHistory {
                    filter { it.version == Version.parse("1") }.shouldBeEmpty()
                    filter { it.description == "RepeatMe" }.shouldBeEmpty()
                    filter { it.version == Version.parse("2") }.shouldBeSingleton {
                        it.description.shouldBe("NotDeleted")
                        it.type.shouldBe(MigrationType.SQL)
                        it.isSuccess.shouldBe(true)
                    }
                }
            }
        }

    @ParameterizedTest
    @ArgumentsSource(DatabasesSupportedByFw::class)
    fun `Does not fail on arbitrary FW schema history`(dbSystem: DbSystem) = (0..50).forEach { schemaHistorySize ->
        val oldSchemaHistoryTable = Names.nextTable().toString()
        withDsl(dbSystem) {
            given {
                database {}
                fwSchemaHistory(oldSchemaHistoryTable, schemaHistorySize)
            }.`when` {
                liberate {
                    withConfig {
                        oldTable(oldSchemaHistoryTable)
                    }
                }
            }.then { actual ->
                actual.oldSchemaHistoryTable.shouldBeEqualIgnoringCase(oldSchemaHistoryTable)
            }
        }
    }

    class DatabasesSupportedByFw : Args(
        functionHasOneParameter = OneParam.YES,
        Sqlite.V3_8_11_2,
        Sqlite.V3_36_0_3,
        Derby.V10_15_2_0,
        MariaDb.V10_6,
        SqlServer.V2019_CU15,
        Hsqldb.V2_6_1,
        Postgres.V14,
        H2.V2_1_210,
        Informix.V14_10,
        Firebird.V4_0_1,
    )

    //
    interface FwSchemaHistorySpec {
        fun entry(version: String?, description: String, type: String, success: Boolean)
    }

    private fun Dsl.GivenStep.fwSchemaHistory(
        table: String,
        block: (FwSchemaHistorySpec).() -> Unit
    ): List<FwAppliedMigration> {
        val result = mutableListOf<FwAppliedMigration>()
        extend { databaseContext ->
            val config = FwConfiguration()
                .dataSource(databaseContext.adminDataSource)
                .table(table)
                .apply { databaseContext.schemaName?.let { schemas("$it") } }
            FwSchemaHistory(config, Random(seed = 0)).use { schemaHistory ->
                schemaHistory.create()
                object : FwSchemaHistorySpec {
                    override fun entry(version: String?, description: String, type: String, success: Boolean) {
                        schemaHistory.add(version, description, type, "script", 0, 0, success)
                    }
                }.block()
                result.addAll(schemaHistory.get())
            }
        }
        return result
    }

    private fun Dsl.GivenStep.fwSchemaHistory(table: String, size: Int): List<FwAppliedMigration> {
        val result = mutableListOf<FwAppliedMigration>()
        extend { databaseContext ->
            val config = FwConfiguration()
                .dataSource(databaseContext.adminDataSource)
                .table(table)
                .apply { databaseContext.schemaName?.let { schemas("$it") } }
            FwSchemaHistory(config, Random(seed = size)).use { schemaHistory ->
                schemaHistory.create()
                val remaining = size - schemaHistory.get().size
                repeat(remaining.coerceAtLeast(0)) {
                    schemaHistory.addRandom()
                }
                result.addAll(schemaHistory.get())
            }
        }
        return result
    }


    @Suppress("DEPRECATION")
    class FwSchemaHistory(private val configuration: FwConfiguration, private val random: Random) : AutoCloseable {
        private val jdbcConnectionFactory = JdbcConnectionFactory(configuration.dataSource, configuration, null)
        private val database = jdbcConnectionFactory.databaseType.createDatabase(
            configuration,
            false,
            jdbcConnectionFactory,
            null
        )
        private val parsingContext = ParsingContext()
        private val sqlScriptFactory =
            jdbcConnectionFactory.databaseType.createSqlScriptFactory(configuration, parsingContext)
        private val noCallbackSqlScriptExecutorFactory =
            jdbcConnectionFactory.databaseType.createSqlScriptExecutorFactory(
                jdbcConnectionFactory,
                NoopCallbackExecutor.INSTANCE,
                null
            )
        private val schemas = SchemaHistoryFactory.prepareSchemas(configuration, database)
        private val defaultSchema = schemas.left
        private val delegate =
            SchemaHistoryFactory.getSchemaHistory(
                configuration,
                noCallbackSqlScriptExecutorFactory,
                sqlScriptFactory,
                database,
                defaultSchema,
                null
            )
        private val versionedTypes = listOf(
            "BASELINE",
            "CUSTOM",
            "UNDO_CUSTOM",
            "UNDO_JDBC",
            "UNDO_SQL",
            "UNDO_SPRING_JDBC",
            "UNDO_SCRIPT",
            "DELETE",
            "JDBC",
            "JDBC_STATE_SCRIPT",
            "SQL",
            "SQL_STATE_SCRIPT",
            "SCRIPT",
            "SPRING_JDBC",
            "SCRIPT_BASELINE",
            "JDBC_BASELINE",
            "SQL_BASELINE"
        )
        private val repeatableTypes = listOf(
            "SQL",
            "SCRIPT",
            "JDBC",
            "SPRING_JDBC",
            "DELETE",
            "CUSTOM",
        )

        fun addRandom() {
            val version: String?
            val type: String
            when (random.nextBoolean()) {
                true -> {     // versioned
                    version = random.nextInt(1, Int.MAX_VALUE).toString()
                    type = versionedTypes.random(random)
                }
                false -> {    // repeatable
                    version = null
                    type = repeatableTypes.random(random)
                }
            }
            val description = randomDescription()
            val script = "script" + random.nextInt()
            val checksum = when (random.nextBoolean()) {
                true -> random.nextInt()
                false -> null
            }
            add(version, description, type, script, checksum, random.nextInt(), random.nextBoolean())
        }

        private fun randomDescription(): String {
            return when (random.nextInt(4)) {
                0 -> ""
                1 -> "D"
                2 -> "__"
                else -> "123"
            }
        }

        fun add(
            version: String?,
            description: String,
            type: String,
            script: String,
            checksum: Int?,
            executionTime: Int,
            success: Boolean
        ) {
            delegate.addAppliedMigration(
                version?.let { FwMigrationVersion.fromVersion(it) },
                description,
                FwMigrationType.CUSTOM,
                script,
                checksum,
                executionTime,
                success
            )
            val table = defaultSchema.getTable(configuration.table)
            val justInserted = get().last().installedRank
            database.mainConnection.jdbcTemplate.update(
                "update $table set ${database.quote("type")} = ?" +
                        " where ${database.quote("installed_rank")}  = ?",
                type, justInserted
            )
        }

        fun create() {
            delegate.create(false)
        }

        fun get(): List<FwAppliedMigration> = delegate.allAppliedMigrations()


        override fun close() {
            database.use { }
        }
    }

    init {
        // Silence!
        LogFactory.setLogCreator { MultiLogger(emptyList()) }
    }
}
