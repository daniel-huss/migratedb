/*
 * Copyright 2022-2024 The MigrateDB contributors
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

import migratedb.v1.core.api.MigrationState.*
import migratedb.v1.core.api.MigrationType
import org.junit.jupiter.api.Test

class BaselineMarkerTest : AbstractMigrationInfoTest() {
    @Test
    fun `Baselined at V3 with pending resolved migrations`() {
        TestCase(
            availableMigrations = listOf("V4"),
            schemaHistory = {
                entry("V1", MigrationType.SQL, true)
                entry("V2", MigrationType.SQL, true)
                entry("V3", MigrationType.BASELINE, true)
            },
            expectedCurrent = "V3",
            expectedNext = "V4",
            expectedApplied = listOf("V1", "V2", "V3"),
            expectedState = mapOf(
                "V1" to MISSING_SUCCESS,
                "V2" to MISSING_SUCCESS,
                "V3" to BASELINE,
                "V4" to PENDING
            )
        )
    }

    @Test
    fun `Baselined at multiple versions`() {
        TestCase(
            availableMigrations = emptyList(),
            schemaHistory = {
                entry("V1", MigrationType.BASELINE, true)
                entry("V2", MigrationType.BASELINE, true)
                entry("V3", MigrationType.BASELINE, true)
            },
            expectedCurrent = "V3",
            expectedNext = null,
            expectedApplied = listOf("V1", "V2", "V3"),
        )
    }

    @Test
    fun `Baselined at V1 with pending repeatable migration`() {
        TestCase(
            availableMigrations = listOf("R__Repeatable"),
            schemaHistory = {
                entry("V1", MigrationType.BASELINE, true)
            },
            expectedCurrent = "V1",
            expectedNext = "R__Repeatable",
            expectedApplied = listOf("V1"),
        )
    }
}
