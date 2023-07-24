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

import migratedb.v1.core.api.MigrationState.DELETED
import migratedb.v1.core.api.MigrationState.IGNORED
import migratedb.v1.core.api.MigrationType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DeletionTest : AbstractMigrationInfoTest() {
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `R__A deleted after being applied`(appliedSuccessfully: Boolean) {
        TestCase(
            availableMigrations = listOf(),
            schemaHistory = {
                entry("R__A", MigrationType.DELETED, appliedSuccessfully)
            },
            expectedApplied = listOf("R__A"),
            expectedCurrent = null,
            expectedNext = null,
            expectedState = mapOf(
                "R__A" to DELETED
            )
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `V1 deleted after being applied`(appliedSuccessfully: Boolean) {
        TestCase(
            availableMigrations = listOf("V1"),
            schemaHistory = {
                entry("V1", MigrationType.DELETED, appliedSuccessfully)
            },
            expectedCurrent = null,
            expectedNext = null,
            expectedState = mapOf(
                "V1" to DELETED
            )
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `V1 deleted before being applied`(appliedSuccessfully: Boolean) {
        // A deleted future migration should not lead to an exception although
        // the repair commmand doesn't mark future migrations as deleted.
        TestCase(
            availableMigrations = listOf(),
            schemaHistory = {
                entry("V1", MigrationType.DELETED, appliedSuccessfully)
            },
            expectedCurrent = null,
            expectedNext = null,
            expectedState = mapOf(
                "V1" to DELETED
            )
        )
    }

    @Test
    fun `V1 deleted, V2 not deleted, V3 deleted`() {
        TestCase(
            availableMigrations = listOf("V1", "V2", "V3"),
            schemaHistory = {
                entry("V1", MigrationType.DELETED, true)
                entry("V3", MigrationType.DELETED, false)
            },
            expectedCurrent = null,
            expectedNext = null,
            expectedState = mapOf(
                "V1" to DELETED,
                "V2" to IGNORED,
                "V3" to DELETED
            )
        )
    }
}
