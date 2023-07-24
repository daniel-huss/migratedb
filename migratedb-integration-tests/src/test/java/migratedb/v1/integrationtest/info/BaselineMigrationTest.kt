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

import migratedb.v1.core.api.MigrationState.IGNORED
import migratedb.v1.core.api.MigrationType
import org.junit.jupiter.api.Test

class BaselineMigrationTest : AbstractMigrationInfoTest() {
    @Test
    fun `B1 is baseline migration`() {
        TestCase(
            availableMigrations = listOf("B1"),
            expectedCurrent = null,
            expectedNext = "B1",
            expectedPending = listOf("B1"),
            expectedApplied = emptyList(),
        )
    }

    @Test
    fun `B1 and B2 are baseline migrations`() {
        TestCase(
            availableMigrations = listOf("B1", "B2"),
            expectedCurrent = null,
            expectedNext = "B2",
            expectedPending = listOf("B2"),
            expectedApplied = emptyList(),
        )
    }

    @Test
    fun `Both incremental and baseline migrations exist`() {
        TestCase(
            availableMigrations = listOf("V1", "B1", "B2", "V2", "V3"),
            expectedAll = listOf("V1", "B2", "V3"),
            expectedCurrent = null,
            expectedNext = "B2",
            expectedPending = listOf("B2", "V3"),
            expectedApplied = emptyList()
        )
    }

    @Test
    fun `Baseline migration B2 exists but B1 has already been applied`() {
        TestCase(
            availableMigrations = listOf("B1", "B2", "V2", "V3"),
            schemaHistory = {
                entry("B1", MigrationType.SQL_BASELINE, true)
            },
            expectedCurrent = "B1",
            expectedNext = "V2",
            expectedPending = listOf("V2", "V3"),
            expectedApplied = listOf("B1"),
        )
    }

    @Test
    fun `Baseline migration B3 exists but is ignored via cherry-picking`() {
        TestCase(
            availableMigrations = listOf("V1", "B2", "B3"),
            configModifier = {
                cherryPick("1", "2")
            },
            expectedAll = listOf("V1", "B2", "B3"),
            expectedCurrent = null,
            expectedNext = "B2",
            expectedPending = listOf("B2"),
            expectedState = mapOf(
                "B3" to IGNORED
            )
        )
    }
}
