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

package migratedb.integrationtest.util.dsl.internal

import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.dsl.Dsl
import migratedb.integrationtest.util.dsl.RunMigrateSpec

class WhenStepImpl<G : Any>(given: G, givenInfo: GivenInfo) : Dsl.WhenStep<G>, AbstractAfterGiven<G>(given, givenInfo) {
    private val executableActions = mutableListOf<() -> Unit>()

    override fun migrate(block: (RunMigrateSpec).() -> Unit) {
        val runMigrate = RunMigrateImpl(givenInfo)
        runMigrate.block()
        executableActions.add(runMigrate::execute)
    }

    override fun arbitraryMutation(): IndependentDatabaseMutation {
        return givenInfo.databaseHandle.nextMutation(givenInfo.schemaName)
    }

    fun executeActions() {
        executableActions.forEach { it() }
    }
}
