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

import migratedb.v1.integrationtest.util.dsl.internal.SimpleJavaMigration
import migratedb.v1.core.api.Checksum
import migratedb.v1.core.api.MigrationState.*
import migratedb.v1.core.api.MigrationType
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
    fun `Only the superseded run has the same checksum`() {
        val oldChecksum = Checksum.parse("oldd")
        val currentChecksum = Checksum.parse("currentt")
        TestCase(
            availableMigrations = listOf(SimpleJavaMigration("R__A", {}, currentChecksum)),
            schemaHistory = {
                entry(null, "A", MigrationType.SQL, true, checksum = currentChecksum)
                entry(null, "A", MigrationType.SQL, true, checksum = oldChecksum)
            },
            expectedStatesInAppliedOrder = mapOf(
                "R__A" to listOf(SUPERSEDED, OUTDATED)
            )
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
