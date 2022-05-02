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

import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.api.migration.JavaMigration
import migratedb.integrationtest.util.base.defaultChecksum
import migratedb.integrationtest.util.dsl.RunWithConfigSpec

abstract class AbstractRunWithConfigSpec(private val givenInfo: GivenInfo) : RunWithConfigSpec {
    private var config = FluentConfiguration().also { cfg ->
        givenInfo.schemaName?.let {
            cfg.schemas(it.toString())
        }
        cfg.skipDefaultResolvers(true)
    }

    override fun createMigrations(names: Collection<String>): Array<JavaMigration> {
        return names.map {
            SimpleJavaMigration(
                name = it,
                code = givenInfo.databaseHandle.nextMutation(givenInfo.schemaName)::apply,
                checksum = it.defaultChecksum()
            )
        }.toTypedArray()
    }

    final override fun withConfig(classLoader: ClassLoader?, block: (FluentConfiguration) -> Unit) {
        config = when (classLoader) {
            null -> FluentConfiguration()
            else -> FluentConfiguration(classLoader)
        }.also(block)
    }

    protected fun <T> execute(block: (config: FluentConfiguration) -> T): T {
        return block(
            FluentConfiguration(config.classLoader)
                .configuration(config)
                .dataSource(givenInfo.databaseHandle.newAdminConnection(givenInfo.namespace))
        )
    }
}
