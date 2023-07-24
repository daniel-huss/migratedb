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

package migratedb.v1.integrationtest.repair

import io.kotest.assertions.asClue
import io.kotest.assertions.print.print
import io.kotest.assertions.withClue
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import migratedb.v1.core.api.Checksum
import migratedb.v1.core.api.MigrationType
import migratedb.v1.core.api.internal.schemahistory.AppliedMigration
import migratedb.v1.core.api.output.RepairOutput
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.util.base.IntegrationTest
import migratedb.v1.integrationtest.util.dsl.internal.SimpleJavaMigration
import migratedb.v1.integrationtest.util.dsl.internal.availableMigrations
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class RepairTest : IntegrationTest() {

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Marks migrations that are no longer available as deleted`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {
                schemaHistory {
                    entry("V1", MigrationType.SQL, true)
                    entry("V2", MigrationType.JDBC, true)
                    entry("R__A", MigrationType.JDBC, true)
                }
            }
        }.`when` {
            repair {
                withConfig {
                    availableMigrations("V3")
                }
            }
        }.then {
            it.migrationsRemoved.shouldBeEmpty()
            it.migrationsAligned.shouldBeEmpty()
            it.migrationsDeleted.versioned().shouldContainExactlyInAnyOrder("1", "2")
            it.migrationsDeleted.repeatable().shouldContainExactly("A")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Does not mark future migrations as deleted`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {
                schemaHistory {
                    entry("V2", MigrationType.SQL, true)
                    entry("R__A", MigrationType.JDBC, true)
                }
            }
        }.`when` {
            repair {
                withConfig {
                    availableMigrations("V1")
                }
            }
        }.then {
            it.migrationsRemoved.shouldBeEmpty()
            it.migrationsAligned.shouldBeEmpty()
            it.migrationsDeleted.versioned().shouldBeEmpty()
            it.migrationsDeleted.repeatable().shouldContainExactly("A")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.JustOneForDebugging::class)
    fun `Removes failed migrations`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {
                schemaHistory {
                    entry("V1", MigrationType.SQL, true)
                    entry("V2", MigrationType.SQL, false)
                    entry("V3", MigrationType.JDBC, false)
                    entry("R__A", MigrationType.JDBC, false)
                }
            }
        }.`when` {
            repair {
                withConfig {
                    availableMigrations("V1", "V2", "V3")
                }
            }
        }.then {
            it.migrationsDeleted.shouldBeEmpty()
            it.migrationsRemoved.repeatable().shouldContainExactly("A")
            it.migrationsRemoved.versioned().shouldContainExactlyInAnyOrder("2", "3")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Aligns checksums of non-deleted successful resolved versioned migrations`(dbSystem: DbSystem) =
        withDsl(dbSystem) {
            val badChecksum = Checksum.parse("badd")
            val goodChecksum = Checksum.parse("good")
            given {
                database {
                    schemaHistory {
                        entry("1", "V1", MigrationType.SQL_BASELINE, true, checksum = badChecksum)
                        entry("2", "V2", MigrationType.DELETED, true, checksum = badChecksum)
                        entry("3", "V3", MigrationType.JDBC, true, checksum = badChecksum)
                        entry("4", "V4", MigrationType.SQL, true, checksum = badChecksum)
                        entry("5", "V5", MigrationType.JDBC_BASELINE, true, checksum = badChecksum)
                        entry("6", "V6", MigrationType.SQL, true, checksum = badChecksum)
                        entry(null, "A", MigrationType.JDBC, true, checksum = goodChecksum)
                        entry(null, "A", MigrationType.JDBC, true, checksum = badChecksum)
                    }
                }
            }.`when` {
                repair {
                    withConfig {
                        val versionedMigrations = (1..5).map { version ->
                            SimpleJavaMigration("V${version}__V${version}", {}, goodChecksum)
                        }
                        val repeatableMigrations = listOf(SimpleJavaMigration("R__A", {}, goodChecksum))
                        availableMigrations(versionedMigrations + repeatableMigrations)
                    }
                }
            }.then { actual ->
                withClue({ actual.print().value }) {
                    actual.migrationsDeleted.shouldBeEmpty()
                    actual.migrationsRemoved.shouldBeEmpty()
                    actual.migrationsRemoved.shouldBeEmpty()
                    actual.migrationsRemoved.repeatable().shouldBeEmpty()
                    actual.migrationsAligned.versioned()
                        .shouldContainExactlyInAnyOrder("1", "3", "4", "5")
                }
                schemaHistory {
                    versioned("1").shouldBeSingleton {
                        it.checksum.shouldBe(goodChecksum)
                    }
                    versioned("2").shouldBeSingleton {
                        it.checksum.shouldBe(badChecksum) // Because it's been deleted
                    }
                    versioned("3").shouldBeSingleton {
                        it.checksum.shouldBe(goodChecksum)
                    }
                    versioned("4").shouldBeSingleton {
                        it.checksum.shouldBe(goodChecksum)
                    }
                    versioned("5").shouldBeSingleton {
                        it.checksum.shouldBe(goodChecksum)
                    }
                    versioned("6").shouldBeSingleton {
                        it.checksum.shouldBe(badChecksum) // Because there's no such resolved migration
                    }
                    repeatable("A").asClue { actual ->
                        // The repeatable ones should not have been touched
                        actual.shouldHaveSize(2)
                        actual.forOne { it.checksum.shouldBe(badChecksum) }
                        actual.forOne { it.checksum.shouldBe(goodChecksum) }
                    }
                }
            }
        }

    private fun List<AppliedMigration>.versioned(version: String) = filter {
        it.version?.toString() == version
    }

    private fun List<AppliedMigration>.repeatable(description: String) = filter {
        it.isExecutionOfRepeatableMigration && it.description == description
    }

    private fun List<RepairOutput>.versioned() = filterNot { it.version.isNullOrBlank() }.map { it.version }
    private fun List<RepairOutput>.repeatable() = filter { it.version.isNullOrBlank() }.map { it.description }
}
