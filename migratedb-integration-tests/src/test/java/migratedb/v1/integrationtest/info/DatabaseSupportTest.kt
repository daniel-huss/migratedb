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

import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.core.api.MigrationState.*
import migratedb.v1.core.api.MigrationType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class DatabaseSupportTest : AbstractMigrationInfoTest() {
    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun `Works on supported database systems`(dbSystem: DbSystem) {
        TestCase(
            dbSystem = dbSystem,
            availableMigrations = listOf("V1", "V2", "V3", "V4", "V5", "V6", "R__A", "R__D", "R__E", "R__F", "R__G"),
            configModifier = {
                cherryPick("1", "2", "3", "4", "5", "A", "B", "C", "D", "E", "F")
            },
            schemaHistory = {
                entry("V1", MigrationType.DELETED, true)
                entry("V2", MigrationType.SQL_BASELINE, true)
                entry("V3", MigrationType.BASELINE, true)
                entry("V4", MigrationType.JDBC, true)
                entry("R__A", MigrationType.SQL, false)
                entry("R__B", MigrationType.JDBC, false)
                entry("R__C", MigrationType.SQL, true)
                entry("R__E", MigrationType.SQL, true, 1)
                entry("R__E", MigrationType.SQL, true, 2)
                entry("R__F", MigrationType.JDBC, true)
            },
            expectedStatesInAppliedOrder = mapOf(
                "V1" to listOf(DELETED),
                "B2" to listOf(SUCCESS),
                "V3" to listOf(BASELINE),
                "V4" to listOf(SUCCESS),
                "V5" to listOf(PENDING),
                "V6" to listOf(IGNORED),
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
