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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.checksum
import migratedb.v1.core.api.configuration.FluentConfiguration
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.core.api.resource.Resource
import migratedb.v1.core.internal.resource.NameListResourceProvider
import migratedb.v1.core.internal.resource.StringResource

/**
 * Makes the given set of migrations resolvable. If this is called more, the effects of the previous invocation are
 * undone.
 *
 * This uses the [FluentConfiguration.javaMigrations] configuration property,
 * so changing that property afterwards will undo the effects of this function.
 */
fun FluentConfiguration.availableMigrations(vararg migrations: Any) =
    availableMigrations(migrations.toList())

/**
 * Makes the given set of migrations resolvable. If this is called more, the effects of the previous invocation are
 * undone.
 *
 * This uses the [FluentConfiguration.javaMigrations] configuration property,
 * so changing that property afterwards will undo the effects of this function.
 */
fun FluentConfiguration.availableMigrations(migrations: List<Any>) = apply {
    migrations.map { nameOrMigration ->
        when (nameOrMigration) {
            is JavaMigration -> nameOrMigration
            is CharSequence -> nameOrMigration.toString().let {
                SimpleJavaMigration(it, {}, it.checksum())
            }
            else -> throw IllegalArgumentException("Don't know how to convert ${nameOrMigration::class.qualifiedName} to a migration")
        }
    }.toTypedArray().let {
        javaMigrations(*it)
    }
}

/**
 * Makes the given set of migrations resolvable. If this is called more, the effects of the previous invocation are
 * undone.
 *
 * This uses the [FluentConfiguration.javaMigrations] and [FluentConfiguration.resourceProvider] configuration property,
 * so changing that property afterwards will undo the effects of this function.
 */
fun FluentConfiguration.availableMigrations(
    scriptMigrations: Collection<ScriptMigration>,
    codeMigrations: Collection<CodeMigration>
) = apply {
    val scriptMap = scriptMigrations.associate {
        val name = "${it.name}.sql"
        name to StringResource(name, it.sql)
    }
    val scriptResourceProvider = object : NameListResourceProvider(scriptMap.keys) {
        override fun toResource(name: String): Resource {
            return scriptMap.getValue(name)
        }
    }
    resourceProvider(scriptResourceProvider)
    javaMigrations(*codeMigrations.map { it.code }.toTypedArray())
}
