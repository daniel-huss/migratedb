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

import migratedb.core.api.MigrationType
import org.junit.jupiter.api.Test

class SimpleCasesTest : MigrationInfoTest() {
    @Test
    fun `Everything empty`() {
        TestCase(
            availableMigrations = listOf(),
            expectedAll = emptyList(),
            expectedApplied = emptyList(),
            expectedCurrent = null,
            expectedNext = null
        )
    }

    @Test
    fun `Single already applied migration`() {
        val single = "V1__One_Already_Applied"
        TestCase(
            availableMigrations = listOf(single),
            schemaHistory = {
                entry(single, MigrationType.SQL, true)
            },
            expectedAll = listOf(single),
            expectedApplied = listOf(single),
        )
    }

    @Test
    fun `Single pending migration`() {
        val single = "V1__One_Already_Applied"
        TestCase(
            availableMigrations = listOf(single),
            expectedAll = listOf(single),
            expectedPending = listOf(single),
            expectedApplied = emptyList(),
        )
    }
}
