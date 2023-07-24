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

package migratedb.v1.integrationtest.info

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.database.SomeInMemoryDb
import migratedb.v1.integrationtest.util.base.IntegrationTest
import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.migrationName
import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.toMigrationName
import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.toMigrationNames
import migratedb.v1.integrationtest.util.dsl.SchemaHistorySpec
import migratedb.v1.integrationtest.util.dsl.internal.availableMigrations
import migratedb.v1.core.api.MigrationInfo
import migratedb.v1.core.api.MigrationInfoService
import migratedb.v1.core.api.MigrationState
import migratedb.v1.core.api.configuration.FluentConfiguration

abstract class AbstractMigrationInfoTest : IntegrationTest() {
    object DoNotCheck : CharSequence by ""

    inner class TestCase(
        private val schemaHistory: (SchemaHistorySpec).() -> Unit = {},
        private val configModifier: (FluentConfiguration).() -> Unit = {},
        dbSystem: DbSystem? = null,
        availableMigrations: List<Any>,
        expectedAll: List<String>? = null,
        expectedPending: List<String>? = null,
        expectedApplied: List<String>? = null,
        expectedResolved: List<String>? = null,
        expectedFailed: List<String>? = null,
        expectedFuture: List<String>? = null,
        expectedOutOfOrder: List<String>? = null,
        expectedOutdated: List<String>? = null,
        expectedCurrent: CharSequence? = DoNotCheck,
        expectedNext: CharSequence? = DoNotCheck,
        expectedState: Map<String, MigrationState>? = null,
        expectedStatesInAppliedOrder: Map<String, List<MigrationState>>? = null,
    ) {
        private val expectedAll = expectedAll.toMigrationNames()
        private val expectedPending = expectedPending.toMigrationNames()
        private val expectedApplied = expectedApplied.toMigrationNames()
        private val expectedResolved = expectedResolved.toMigrationNames()
        private val expectedFailed = expectedFailed.toMigrationNames()
        private val expectedFuture = expectedFuture.toMigrationNames()
        private val expectedOutOfOrder = expectedOutOfOrder.toMigrationNames()
        private val expectedOutdated = expectedOutdated.toMigrationNames()
        private val expectedCurrent = expectedCurrent?.toMigrationNameUnlessDoNotCheck()
        private val expectedNext = expectedNext?.toMigrationNameUnlessDoNotCheck()
        private val expectedState = expectedState?.mapKeys { (k, _) -> k.toMigrationName() }
        private val expectedStatesInAppliedOrder =
            expectedStatesInAppliedOrder?.mapKeys { (k, _) -> k.toMigrationName() }

        init {
            withDsl(dbSystem ?: SomeInMemoryDb) {
                given {
                    database {
                        schemaHistory {
                            this.apply(schemaHistory)
                        }
                    }
                }.`when` {
                    info {
                        withConfig {
                            configModifier()
                            availableMigrations(availableMigrations)
                        }
                    }
                }.then { actual ->
                    verify(actual)
                }
            }
        }

        private fun verify(actual: MigrationInfoService) {
            assertSoftly {
                withClue("all()") {
                    expectedAll?.let { actual.all().shouldMatch(it) }
                }
                withClue("pending()") {
                    expectedPending?.let { actual.pending().shouldMatch(it) }
                }
                withClue("applied") {
                    expectedApplied?.let { actual.applied().shouldMatch(it) }
                }
                withClue("resolved()") {
                    expectedResolved?.let { actual.resolved().shouldMatch(it) }
                }
                withClue("failed()") {
                    expectedFailed?.let { actual.failed().shouldMatch(it) }
                }
                withClue("future()") {
                    expectedFuture?.let { actual.future().shouldMatch(it) }
                }
                withClue("outOfOrder()") {
                    expectedOutOfOrder?.let { actual.outOfOrder().shouldMatch(it) }
                }
                withClue("expectedOutdated()") {
                    expectedOutdated?.let { actual.outdated().shouldMatch(it) }
                }
                withClue("current()") {
                    when {
                        expectedCurrent == null -> actual.current().shouldBeNull()
                        expectedCurrent === DoNotCheck -> {}
                        else -> arrayOf(actual.current()).shouldMatch(listOf(expectedCurrent.toString()))
                    }
                }
                withClue("next()") {
                    when {
                        expectedNext == null -> actual.next().shouldBeNull()
                        expectedNext === DoNotCheck -> {}
                        else -> arrayOf(actual.next()).shouldMatch(listOf(expectedNext.toString()))
                    }
                }
            }
            withClue("state") {
                val grouped = actual.all()
                    .groupBy { it.migrationName() }
                    .mapValues { (_, v) -> v.map { it.state }.toList() }

                expectedState?.let { expected ->
                    grouped.entries.firstOrNull { (_, v) -> v.toSet().size > 1 }?.let {
                        fail(
                            "Cannot use expectedState because at least one version has multiple ocurrences." +
                                    " Use expectedStatesInAppliedOrder.\n$it"
                        )
                    }
                    grouped.mapValues { (_, v) -> v.first() }
                        .shouldContainAll(expected)
                }

                expectedStatesInAppliedOrder?.let { expected ->
                    grouped.shouldContainAll(expected)
                }
            }
        }

        private fun CharSequence.toMigrationNameUnlessDoNotCheck() = when (this) {
            DoNotCheck -> DoNotCheck
            else -> toString().toMigrationName()
        }

        private fun Array<MigrationInfo?>.shouldMatch(names: List<String>) {
            shouldHaveSize(names.size)
            forEachIndexed { index, actual ->
                actual.shouldNotBeNull()
                val actualDescription = actual.migrationName()
                val expected = names[index]
                withClue("Expected = $expected | Actual = $actualDescription") {
                    if (!actual.matches(names[index])) {
                        fail("Should match")
                    }
                }
            }
        }

        private fun MigrationInfo.matches(name: String): Boolean {
            return name == migrationName()
        }
    }
}
