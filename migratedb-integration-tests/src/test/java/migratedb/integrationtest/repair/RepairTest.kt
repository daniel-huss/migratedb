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

package migratedb.integrationtest.repair

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import migratedb.core.api.MigrationType
import migratedb.core.api.output.RepairOutput
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class RepairTest : IntegrationTest() {

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Deletes migrations that are no longer available`(dbSystem: DbSystem) = withDsl(dbSystem) {
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
                availableMigrations("V3")
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
    fun `Does not delete future migrations`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {
                schemaHistory {
                    entry("V2", MigrationType.SQL, true)
                    entry("R__A", MigrationType.JDBC, true)
                }
            }
        }.`when` {
            repair {
                availableMigrations("V1")
            }
        }.then {
            it.migrationsRemoved.shouldBeEmpty()
            it.migrationsAligned.shouldBeEmpty()
            it.migrationsDeleted.versioned().shouldBeEmpty()
            it.migrationsDeleted.repeatable().shouldContainExactly("A")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
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
                availableMigrations("V1", "V2", "V3")
            }
        }.then {
            it.migrationsDeleted.shouldBeEmpty()
            it.migrationsRemoved.versioned().shouldContainExactlyInAnyOrder("2", "3")
            it.migrationsRemoved.repeatable().shouldContainExactly("A")
        }
    }

    private fun List<RepairOutput>.versioned() = mapNotNull { it.version.takeUnless(String::isBlank) }
    private fun List<RepairOutput>.repeatable() = filter { it.version.isNullOrBlank() }.map { it.description }
}
