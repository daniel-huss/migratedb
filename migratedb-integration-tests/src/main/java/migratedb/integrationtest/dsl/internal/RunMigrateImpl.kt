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

package migratedb.integrationtest.dsl.internal

import migratedb.core.api.MigrationVersion
import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.api.migration.Context
import migratedb.core.api.migration.JavaMigration
import migratedb.core.api.resource.Resource
import migratedb.core.internal.resolver.MigrationInfoHelper.extractVersionAndDescription
import migratedb.core.internal.resource.NameListResourceProvider
import migratedb.core.internal.resource.StringResource
import migratedb.integrationtest.dsl.RunMigrateSpec
import java.sql.Connection

class RunMigrateImpl(private val givenInfo: GivenInfo) : RunMigrateSpec {
    data class ScriptMigration(val name: String, val sql: String)
    data class CodeMigration(val name: String, val code: JavaMigration)

    private val scriptMigrations = mutableListOf<ScriptMigration>()
    private val codeMigrations = mutableListOf<CodeMigration>()
    private var config = FluentConfiguration().schemas(givenInfo.schemaName.toString())

    override fun script(name: String, sql: String) {
        scriptMigrations.add(ScriptMigration(name, sql))
    }

    override fun code(name: String, code: (Connection) -> Unit) {
        codeMigrations.add(CodeMigration(name, object : JavaMigration {
            private val version: MigrationVersion
            private val description: String
            private val prefix = name[0].uppercase()

            init {
                extractVersionAndDescription(name, prefix, "__", prefix == "R").let {
                    version = it.version!!
                    description = it.description
                }
            }

            override fun getVersion(): MigrationVersion = version
            override fun getDescription(): String = description
            override fun getChecksum(): Int? = null
            override fun isUndo(): Boolean = prefix == "U"
            override fun isStateScript(): Boolean = false
            override fun canExecuteInTransaction() = true
            override fun migrate(context: Context) {
                code(context.connection)
            }
        }))
    }

    override fun code(name: String, code: JavaMigration) {
        codeMigrations.add(CodeMigration(name, code))
    }

    override fun code(name: String) = code(name) {
        givenInfo.databaseHandle.nextMutation(givenInfo.schemaName).apply(it)
    }

    override fun config(classLoader: ClassLoader?, block: (FluentConfiguration) -> Unit) {
        config = when (classLoader) {
            null -> FluentConfiguration()
            else -> FluentConfiguration(classLoader)
        }.also(block)
    }

    fun execute() {
        val scriptMap = scriptMigrations.associate {
            "${it.name}.sql" to StringResource(it.sql)
        }
        FluentConfiguration(config.classLoader)
            .configuration(config)
            .dataSource(
                givenInfo.databaseHandle.newAdminConnection(
                    givenInfo.databaseName,
                    givenInfo.schemaName
                )
            )
            .javaMigrations(*codeMigrations.map { it.code }.toTypedArray())
            .resourceProvider(object : NameListResourceProvider(scriptMap.keys) {
                override fun toResource(name: String): Resource {
                    return scriptMap.getValue(name)
                }
            })
            .load()
            .migrate()
    }
}
