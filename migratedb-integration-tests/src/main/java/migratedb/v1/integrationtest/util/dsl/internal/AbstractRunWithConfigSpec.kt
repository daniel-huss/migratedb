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

import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.checksum
import migratedb.v1.integrationtest.util.dsl.RunWithConfigSpec
import migratedb.v1.core.api.configuration.FluentConfiguration
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.core.internal.util.ClassUtils

abstract class AbstractRunWithConfigSpec(private val databaseContext: DatabaseContext) : RunWithConfigSpec {
    private var config = newConfig()

    override fun createMigrations(names: Collection<String>): Array<JavaMigration> {
        return names.map {
            SimpleJavaMigration(
                name = it,
                code = databaseContext.databaseInstance.nextMutation(databaseContext.schemaName)::apply,
                checksum = it.checksum()
            )
        }.toTypedArray()
    }

    final override fun <T> withConfig(classLoader: ClassLoader?, block: (FluentConfiguration).() -> T): T {
        config = newConfig(classLoader)
        return block(config)
    }

    protected fun <T> execute(block: (config: FluentConfiguration) -> T): T {
        return block(
            FluentConfiguration(config.classLoader)
                .configuration(config)
                .dataSource(databaseContext.databaseInstance.newAdminConnection(databaseContext.namespace))
        )
    }


    private fun newConfig(classLoader: ClassLoader? = null) =
        FluentConfiguration(classLoader ?: ClassUtils.defaultClassLoader())
            .also { cfg ->
                databaseContext.schemaName?.let {
                    cfg.schemas(it.toString())
                }
            }
}
