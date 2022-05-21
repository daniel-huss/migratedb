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

import migratedb.core.api.output.RepairResult
import migratedb.integrationtest.util.dsl.RunRepairSpec

class RunRepairImpl(private val givenInfo: GivenInfo) : RunRepairSpec, AbstractRunWithConfigSpec(givenInfo) {
    private var availableMigrations = emptyList<CodeMigration>()

    /**
     * Specifies which resolved migrations are visible to the repair command (shortened sytnax like "V1" is allowed).
     */
    override fun availableMigrations(vararg names: String) {
        availableMigrations = names.map {
            CodeMigration(
                it,
                SimpleJavaMigration(it, givenInfo.databaseHandle.nextMutation(givenInfo.schemaName)::apply)
            )
        }
    }

    fun execute(): RepairResult = execute { config ->
        config.availableMigrations(emptyList(), availableMigrations)
            .load()
            .repair()
    }
}
