/*
 * Copyright 2022-2023 The MigrateDB contributors
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

import migratedb.v1.integrationtest.util.dsl.Dsl.Companion.toMigrationName
import migratedb.v1.core.api.Checksum
import migratedb.v1.core.api.Version
import migratedb.v1.core.api.configuration.Configuration
import migratedb.v1.core.api.migration.Context
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.core.internal.resolver.MigrationInfoHelper
import java.sql.Connection

class SimpleJavaMigration(
    name: String,
    private val code: (Connection) -> Unit,
    private val checksum: Checksum? = null
) : JavaMigration {
    private val version: Version?
    private val description: String
    private val prefix = name[0].uppercase()

    init {
        MigrationInfoHelper.extractVersionAndDescription(
            name.toMigrationName(),
            prefix,
            "__",
            prefix == "R"
        ).also {
            version = it.version
            description = it.description
        }
    }

    override fun getVersion(): Version? = version
    override fun getDescription(): String = description

    override fun getChecksum(configuration: Configuration?) = checksum

    override fun isBaselineMigration(): Boolean = prefix == "B"
    override fun canExecuteInTransaction() = true
    override fun migrate(context: Context) {
        code(context.connection)
    }

    override fun toString(): String {
        return "$prefix${version ?: ""}__${description.replace(' ', '_')}"
    }
}
