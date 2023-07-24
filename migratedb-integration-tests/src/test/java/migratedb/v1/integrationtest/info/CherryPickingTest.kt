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
import migratedb.v1.core.api.MigrationState.PENDING
import migratedb.v1.core.api.MigrationType
import org.junit.jupiter.api.Test

class CherryPickingTest : AbstractMigrationInfoTest() {
    @Test
    fun `Cherry-pick list is empty, which means everything is included`() {
        TestCase(
            availableMigrations = listOf("V1", "V2"),
            configModifier = {
                cherryPick(*emptyArray<String>())
            },
            expectedPending = null,
            expectedState = mapOf(
                "V1" to PENDING,
                "V2" to PENDING
            )
        )
    }

    @Test
    fun `Cherry-pick list contains only names of unresolved migrations`() {
        TestCase(
            availableMigrations = listOf("V1", "V2"),
            schemaHistory = {
                entry("V2", MigrationType.SQL, true)
            },
            configModifier = {
                cherryPick("V3")
            },
            expectedState = mapOf(
                "V1" to IGNORED,
                "V2" to IGNORED // Apparently ignored takes precedence over successfully applied 🤷
            )
        )
    }

    @Test
    fun `All migrations are included`() {
        TestCase(
            availableMigrations = listOf("B1", "V2", "R__Repeatable"),
            configModifier = {
                cherryPick("1", "2", "Repeatable")
            },
            expectedState = mapOf(
                "B1" to PENDING,
                "V2" to PENDING,
                "R__Repeatable" to PENDING
            )
        )
    }

    @Test
    fun `V1 and V3 are cherry-picked, but V2 is not`() {
        TestCase(
            availableMigrations = listOf("V1", "V2", "V3"),
            configModifier = {
                cherryPick("1", "3")
            },
            expectedState = mapOf(
                "V1" to PENDING,
                "V2" to IGNORED,
                "V3" to PENDING
            )
        )
    }

    @Test
    fun `A and C are cherry-picked, but B is not`() {
        TestCase(
            availableMigrations = listOf("R__A", "R__B", "R__C"),
            configModifier = {
                cherryPick("A", "C")
            },
            expectedState = mapOf(
                "R__A" to PENDING,
                "R__B" to IGNORED,
                "R__C" to PENDING
            )
        )
    }
}
