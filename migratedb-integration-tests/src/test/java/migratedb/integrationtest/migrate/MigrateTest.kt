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

package migratedb.integrationtest.migrate

import io.kotest.assertions.asClue
import io.kotest.assertions.print.print
import io.kotest.assertions.withClue
import io.kotest.inspectors.forOne
import io.kotest.inspectors.shouldForExactly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import migratedb.core.api.MigrateDbException
import migratedb.core.api.MigrationType
import migratedb.core.api.Version
import migratedb.core.api.configuration.ClassicConfiguration
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.IntegrationTest
import migratedb.integrationtest.util.dsl.DatabasesSupportedByFw
import migratedb.integrationtest.util.dsl.fwSchemaHistory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.sql.Connection

internal class MigrateTest : IntegrationTest() {

    @ParameterizedTest
    @ArgumentsSource(DatabasesSupportedByFw::class)
    fun `LiberateOnMigrate works`(dbSystem: DbSystem) = withDsl(dbSystem) {
        val oldSchemaHistoryTable = ClassicConfiguration().oldTable
        given {
            database { }
            fwSchemaHistory(oldSchemaHistoryTable) {
                entry(version = "1", description = "Foo", type = "JDBC", success = true)
                entry(version = "2", description = "Bar", type = "JDBC", success = true)
            }
        }.`when` {
            migrate {
                withConfig { liberateOnMigrate(true) }
                usingCode("V1__Foo", arbitraryMutation())
                usingCode("V2__Bar", arbitraryMutation())
                usingCode("V3__Baz", arbitraryMutation())
            }
        }.then { actual ->
            withClue(actual.print().value) {
                actual.liberateResult.shouldNotBeNull().apply {
                    oldSchemaHistoryTable.shouldBeEqualIgnoringCase(oldSchemaHistoryTable)
                    actions.shouldForExactly(2) {
                        it.type.shouldBe("copied_jdbc_migration")
                    }
                }
                actual.migrationsExecuted.shouldBe(1)
                actual.migrations.shouldBeSingleton {
                    it.version.shouldBe("3")
                    it.description.shouldBe("Baz")
                }
                schemaHistory {
                    forOne {
                        it.version.shouldBe(Version.parse("1"))
                        it.description.shouldBe("Foo")
                        withClue("Success?") {
                            it.isSuccess.shouldBeTrue()
                        }
                    }
                    forOne {
                        it.version.shouldBe(Version.parse("2"))
                        it.description.shouldBe("Bar")
                        withClue("Success?") {
                            it.isSuccess.shouldBeTrue()
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Can apply a single migration against an empty database`(dbSystem: DbSystem) = withDsl(dbSystem) {
        data class Migrations(val v1: IndependentDatabaseMutation)

        given {
            database { }
            Migrations(v1 = independentDbMutation())
        }.`when` {
            migrate {
                usingCode("V1", given.v1)
            }
        }.then { actual ->
            withClue(actual.print().value) {
                actual.migrationsExecuted.shouldBe(1)
                actual.targetSchemaVersion.shouldBe("1")
                actual.warnings?.shouldBeEmpty()
                actual.migrations.shouldBeSingleton {
                    it.version.shouldBe("1")
                }
                schemaHistory {
                    forOne {
                        it.version.shouldBe(Version.parse("1"))
                        withClue("Success?") {
                            it.isSuccess.shouldBeTrue()
                        }
                    }
                }
                withClue("V1 applied?") {
                    given.v1.shouldBeApplied()
                }
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Failed migration does not undo previous succeeding migration`(dbSystem: DbSystem) = withDsl(dbSystem) {
        data class Migrations(val v2: IndependentDatabaseMutation, val v3: (Connection) -> Unit)

        given {
            database {
                schemaHistory {
                    entry("V1", MigrationType.SQL, true)
                }
            }
            Migrations(v2 = independentDbMutation(), v3 = { throw MigrateDbException("Failed") })
        }.`when` {
            runCatching {
                migrate {
                    withConfig {
                        ignoreMissingMigrations(true)
                    }
                    usingCode("V2", given.v2)
                    usingCode("V3", given.v3)
                }
            }
        }.then { actual ->
            actual.asClue {
                actual.shouldBeFailure<MigrateDbException>()
                schemaHistory {
                    forOne {
                        it.version.shouldBe(Version.parse("1"))
                        withClue("V1 successful?") {
                            it.isSuccess.shouldBeTrue()
                        }
                    }
                    forOne {
                        it.version.shouldBe(Version.parse("2"))
                        withClue("V2 successful?") {
                            it.isSuccess.shouldBeTrue()
                        }
                    }
                    singleOrNull { it.version == Version.parse("3") }?.asClue {
                        it.isSuccess.shouldBeFalse()
                    }
                    withClue("V2 still applied?") {
                        given.v2.shouldBeApplied()
                    }
                }
            }
        }
    }
}
