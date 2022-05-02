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

import migratedb.core.api.MigrationState.FAILED
import migratedb.core.api.MigrationState.IGNORED
import migratedb.core.api.MigrationState.MISSING_FAILED
import migratedb.core.api.MigrationState.MISSING_SUCCESS
import migratedb.core.api.MigrationState.OUTDATED
import migratedb.core.api.MigrationState.PENDING
import migratedb.core.api.MigrationState.SUCCESS
import migratedb.core.api.MigrationState.SUPERSEDED
import migratedb.core.api.MigrationType
import org.junit.jupiter.api.Test

class RepeatableMigrationTest : AbstractMigrationInfoTest() {
    @Test
    fun `History contains superseded executions`() {
        TestCase(
            availableMigrations = listOf("R__A"),
            schemaHistory = {
                entry("R__A", MigrationType.JDBC, true, 1)
                entry("R__A", MigrationType.JDBC, false, 2)
                entry("R__A", MigrationType.JDBC, true, 3)
            },
            expectedStatesInAppliedOrder = mapOf(
                "R__A" to listOf(SUPERSEDED, SUPERSEDED, OUTDATED)
            ),
            expectedCurrent = "R__A",
            expectedNext = "R__A"
        )
    }

    @Test
    fun `Every possible repeatable migration state`() {
        TestCase(
            availableMigrations = listOf("R__A", "R__D", "R__E", "R__F", "R__G"),
            configModifier = {
                cherryPick("A", "B", "C", "D", "E", "F")
            },
            schemaHistory = {
                entry("R__A", MigrationType.SQL, false)
                entry("R__B", MigrationType.JDBC, false)
                entry("R__C", MigrationType.SQL, true)
                entry("R__E", MigrationType.SQL, true, 1)
                entry("R__E", MigrationType.SQL, true, 2)
                entry("R__F", MigrationType.JDBC, true)
            },
            expectedStatesInAppliedOrder = mapOf(
                "R__A" to listOf(FAILED),
                "R__B" to listOf(MISSING_FAILED),
                "R__C" to listOf(MISSING_SUCCESS),
                "R__D" to listOf(PENDING),
                "R__E" to listOf(SUPERSEDED, OUTDATED),
                "R__F" to listOf(SUCCESS),
                "R__G" to listOf(IGNORED)
            )
        )
    }
}
