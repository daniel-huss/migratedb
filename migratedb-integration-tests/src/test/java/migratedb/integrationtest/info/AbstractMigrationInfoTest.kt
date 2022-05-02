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

package migratedb.integrationtest.info

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import migratedb.core.api.MigrationInfo
import migratedb.core.api.MigrationInfoService
import migratedb.core.api.MigrationState
import migratedb.core.api.MigrationType
import migratedb.core.api.Version
import migratedb.core.api.configuration.FluentConfiguration
import migratedb.integrationtest.database.SomeInMemoryDb
import migratedb.integrationtest.util.base.IntegrationTest
import migratedb.integrationtest.util.base.defaultChecksum

abstract class AbstractMigrationInfoTest : IntegrationTest() {
    object DoNotCheck : CharSequence by ""

    interface SimpleSchemaHistorySpec {
        fun entry(name: String, type: MigrationType, success: Boolean, checksumDelta: Int = 0)
    }

    inner class TestCase(
        private val schemaHistory: (SimpleSchemaHistorySpec).() -> Unit = {},
        private val configModifier: (FluentConfiguration).() -> Unit = {},
        availableMigrations: List<String>,
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
        private val expectedAll = expectedAll.addDescriptionIfMissing()
        private val expectedPending = expectedPending.addDescriptionIfMissing()
        private val expectedApplied = expectedApplied.addDescriptionIfMissing()
        private val expectedResolved = expectedResolved.addDescriptionIfMissing()
        private val expectedFailed = expectedFailed.addDescriptionIfMissing()
        private val expectedFuture = expectedFuture.addDescriptionIfMissing()
        private val expectedOutOfOrder = expectedOutOfOrder.addDescriptionIfMissing()
        private val expectedOutdated = expectedOutdated.addDescriptionIfMissing()
        private val expectedCurrent = expectedCurrent?.addDescriptionIfMissing()
        private val expectedNext = expectedNext?.addDescriptionIfMissing()
        private val expectedState = expectedState?.mapKeys { (k, _) -> k.addDescriptionIfMissing() }
        private val expectedStatesInAppliedOrder = expectedStatesInAppliedOrder?.mapKeys { (k, _) -> k.addDescriptionIfMissing() }

        init {
            withDsl(SomeInMemoryDb) {
                given {
                    database {
                        existingSchemaHistory {
                            val history = this
                            object : SimpleSchemaHistorySpec {
                                override fun entry(name: String, type: MigrationType, success: Boolean, checksumDelta: Int) {
                                    val nameWithDescription = name.addDescriptionIfMissing()
                                    history.entry(
                                        name = nameWithDescription,
                                        type = type,
                                        success = success,
                                        checksum = nameWithDescription.defaultChecksum() + checksumDelta
                                    )
                                }
                            }.apply(schemaHistory)
                        }
                    }
                }.`when` {
                    info {
                        withConfig {
                            configModifier.invoke(it)
                            val migrations = createMigrations(availableMigrations.addDescriptionIfMissing())
                            it.javaMigrations(*migrations)
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
                    .groupBy { it.describe() }
                    .mapValues { (_, v) -> v.map { it.state }.toList() }

                expectedState?.let { expected ->
                    grouped.entries.firstOrNull { (_, v) -> v.toSet().size > 1 }?.let {
                        fail(
                            "Cannot use expectedState because at least one migration has multiple ocurrences." +
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

        private fun CharSequence.addDescriptionIfMissing() = when (this) {
            DoNotCheck -> DoNotCheck
            else -> toString().addDescriptionIfMissing()
        }

        private fun String.addDescriptionIfMissing() = when {
            !contains("__") -> "${this}__V${this.drop(1)}"
            else -> this
        }

        private fun List<String>.addDescriptionIfMissing() = this.map { it.addDescriptionIfMissing() }

        @JvmName("addDescriptionIfMissingOrNull")
        private fun List<String>?.addDescriptionIfMissing() = this?.map { it.addDescriptionIfMissing() }

        private fun Array<MigrationInfo?>.shouldMatch(names: List<String>) {
            shouldHaveSize(names.size)
            forEachIndexed { index, actual ->
                actual.shouldNotBeNull()
                val actualDescription = actual.describe()
                val expected = names[index]
                withClue("Expected = $expected | Actual = $actualDescription") {
                    if (!actual.matches(names[index])) {
                        fail("Should match")
                    }
                }
            }
        }

        private fun MigrationInfo.matches(name: String): Boolean {
            return name == describe()
        }

        private fun MigrationInfo.describe() = when {
            isRepeatable -> "R__" + description.spaceToUnderscore()
            else -> (if (type.isBaselineMigration) "B" else "V") +
                    version?.dotToUnderscrore() + "__" + description.spaceToUnderscore()
        }

        private fun Version.dotToUnderscrore() = toString().replace('.', '_')
        private fun String.spaceToUnderscore() = replace(' ', '_')
    }
}
