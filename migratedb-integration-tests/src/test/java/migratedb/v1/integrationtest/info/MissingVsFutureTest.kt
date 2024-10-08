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

class MissingVsFutureTest : AbstractMigrationInfoTest() {
    @Test
    fun `Successful and failed applied migrations with no resolved migrations`() {
        TestCase(
            availableMigrations = emptyList(),
            schemaHistory = {
                entry("V1", MigrationType.SQL, true)
                entry("V2", MigrationType.SQL, false)
            },
            expectedState = mapOf(
                "V1" to FUTURE_SUCCESS,
                "V2" to FUTURE_FAILED
            )
        )
    }

    @Test
    fun `Both missing and future applied migrations exist`() {
        TestCase(
            availableMigrations = listOf("V3", "V4"),
            schemaHistory = {
                entry("V1", MigrationType.JDBC, true)
                entry("V2", MigrationType.SQL, false)
                entry("V5", MigrationType.SQL, true)
                entry("V6", MigrationType.JDBC, false)
            },
            expectedState = mapOf(
                "V1" to MISSING_SUCCESS,
                "V2" to MISSING_FAILED,
                "V3" to IGNORED,
                "V4" to IGNORED,
                "V5" to FUTURE_SUCCESS,
                "V6" to FUTURE_FAILED
            )
        )
    }
}
