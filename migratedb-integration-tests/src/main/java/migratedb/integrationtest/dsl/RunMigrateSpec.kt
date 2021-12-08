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

package migratedb.integrationtest.dsl

import java.sql.Connection

class RunMigrateSpec {
    data class MigrationScript(val name: String, val sql: String)
    data class MigrationCode(val name: String, val code: (Connection) -> Unit)

    private val scriptMigrations = mutableListOf<MigrationScript>()
    private val codeMigrations = mutableListOf<MigrationCode>()

    fun migration(name: String, sql: String) = scriptMigrations.add(MigrationScript(name, sql))
    fun migration(name: String, code: (Connection) -> Unit) = codeMigrations.add(MigrationCode(name, code))
}
