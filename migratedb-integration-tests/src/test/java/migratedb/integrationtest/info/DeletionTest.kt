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

import migratedb.core.api.MigrationState.DELETED
import migratedb.core.api.MigrationType
import org.junit.jupiter.api.Test

class DeletionTest : AbstractMigrationInfoTest() {
    @Test
    fun `V1 deleted after being applied`() {
        TestCase(
            availableMigrations = listOf(),
            schemaHistory = {
                entry("V1", MigrationType.SQL, true)
                entry("V1", MigrationType.DELETED, true)
            },
            // Surprising, but the forked project didn't report deleted applied migrations here
            expectedCurrent = null,
            expectedNext = null,
            expectedState = mapOf(
                "V1" to DELETED
            )
        )
    }
}
