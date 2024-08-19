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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.integrationtest.util.dsl.RunMigrateSpec
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.core.api.output.MigrateResult
import java.sql.Connection

class RunMigrateImpl(private val databaseContext: DatabaseContext) : AbstractRunWithConfigSpec(databaseContext),
    RunMigrateSpec {
    private val scriptMigrations = mutableListOf<ScriptMigration>()
    private val codeMigrations = mutableListOf<CodeMigration>()

    override fun usingScript(name: String, sql: String) {
        scriptMigrations.add(ScriptMigration(name, sql))
    }

    override fun usingCode(name: String, code: (Connection) -> Unit) {
        codeMigrations.add(CodeMigration(name, SimpleJavaMigration(name, code)))
    }

    override fun usingCode(name: String, code: JavaMigration) {
        codeMigrations.add(CodeMigration(name, code))
    }

    override fun usingCode(name: String) = usingCode(name) {
        databaseContext.databaseInstance.nextMutation(databaseContext.schemaName).apply(it)
    }

    fun execute(): MigrateResult = execute { config ->
        config.availableMigrations(scriptMigrations, codeMigrations)
            .load()
            .migrate()
    }
}

