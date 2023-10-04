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

package migratedb.v1.integrationtest.database.mutation

import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import java.sql.Connection

/**
 * Creates / drops a table in a schema. Works with SQL databases that support `information_schema.tables`.
 */
class BasicCreateTableMutation(private val normalizedSchema: SafeIdentifier?, private val normalizedTable: SafeIdentifier) :
    IndependentDatabaseMutation {

    private val qualifiedTable = when (normalizedSchema) {
        null -> normalizedTable
        else -> "$normalizedSchema.$normalizedTable"
    }

    override fun isApplied(connection: Connection): Boolean {
        return connection.work(commit = false) {
            var query = "select table_name from information_schema.tables where table_name = '$normalizedTable'"
            if (normalizedSchema != null) {
                query += " and table_schema = '$normalizedSchema'"
            }
            it.query(query) { _, _ -> true }.isNotEmpty()
        }
    }

    override fun apply(connection: Connection) {
        connection.work(commit = true) {
            it.execute("create table $qualifiedTable(id int not null primary key)")
        }
    }

    override fun undo(connection: Connection) {
        connection.work(commit = true) {
            it.execute("drop table $qualifiedTable")
        }
    }
}
