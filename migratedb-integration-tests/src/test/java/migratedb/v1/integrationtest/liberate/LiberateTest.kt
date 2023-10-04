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

package migratedb.v1.integrationtest.liberate

import io.kotest.inspectors.shouldForExactly
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import migratedb.v1.integrationtest.database.*
import migratedb.v1.integrationtest.util.base.IntegrationTest
import migratedb.v1.integrationtest.util.base.Names
import migratedb.v1.integrationtest.util.dsl.DatabasesSupportedByFw
import migratedb.v1.integrationtest.util.dsl.fwSchemaHistory
import migratedb.v1.core.api.MigrationType
import migratedb.v1.core.api.Version
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

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
}
