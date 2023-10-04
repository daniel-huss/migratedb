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

package migratedb.v1.integrationtest.baseline

import io.kotest.assertions.print.print
import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.util.base.IntegrationTest
import migratedb.v1.core.api.MigrationType
import migratedb.v1.core.api.Version
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class BaselineTest : IntegrationTest() {
    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Can baseline an empty database`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {}
        }.`when` {
            baseline {
                withConfig {
                    baselineVersion("82")
                }
            }
        }.then { actual ->
            withClue(actual.print().value) {
                actual.successfullyBaselined.shouldBeTrue()
                actual.baselineVersion.shouldBe("82")
                actual.warnings?.shouldBeEmpty()
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Can baseline a non-empty database`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database {
                initializedBy {
                    independentDbMutation().apply(it)
                }
            }
        }.`when` {
            baseline {
                withConfig {
                    baselineVersion("1")
                }
            }
        }.then { actual ->
            withClue(actual.print().value) {
                actual.successfullyBaselined.shouldBeTrue()
                actual.baselineVersion.shouldBe("1")
                actual.warnings?.shouldBeEmpty()
            }
            schemaHistory {
                shouldForOne {
                    it.version.shouldBe(Version.parse("1"))
                    it.type.shouldBe(MigrationType.BASELINE)
                    it.isSuccess.shouldBeTrue()
                }
            }
        }
    }
}
