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

import migratedb.core.api.migration.JavaMigration
import migratedb.core.api.output.MigrateResult
import migratedb.core.api.resource.Resource
import migratedb.core.internal.resource.NameListResourceProvider
import migratedb.core.internal.resource.StringResource
import migratedb.integrationtest.util.dsl.RunMigrateSpec
import java.sql.Connection

class RunMigrateImpl(private val givenInfo: GivenInfo) : AbstractRunWithConfigSpec(givenInfo), RunMigrateSpec {
    data class ScriptMigration(val name: String, val sql: String)
    data class CodeMigration(val name: String, val code: JavaMigration)

    private val scriptMigrations = mutableListOf<ScriptMigration>()
    private val codeMigrations = mutableListOf<CodeMigration>()

    override fun script(name: String, sql: String) {
        scriptMigrations.add(ScriptMigration(name, sql))
    }

    override fun code(name: String, code: (Connection) -> Unit) {
        codeMigrations.add(CodeMigration(name, SimpleJavaMigration(name, code)))
    }

    override fun code(name: String, code: JavaMigration) {
        codeMigrations.add(CodeMigration(name, code))
    }

    override fun code(name: String) = code(name) {
        givenInfo.databaseHandle.nextMutation(givenInfo.schemaName).apply(it)
    }

    fun execute(): MigrateResult = execute { config ->
        val scriptMap = scriptMigrations.associate {
            val name = "${it.name}.sql"
            name to StringResource(name, it.sql)
        }
        config.javaMigrations(*codeMigrations.map { it.code }.toTypedArray())
            .resourceProvider(object : NameListResourceProvider(scriptMap.keys) {
                override fun toResource(name: String): Resource {
                    return scriptMap.getValue(name)
                }
            })
            .load()
            .migrate()
    }
}

