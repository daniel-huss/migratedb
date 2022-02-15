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

package migratedb.integrationtest.database

import migratedb.integrationtest.SafeIdentifier
import migratedb.integrationtest.with
import java.sql.Connection

/**
 * Creates / drops a table whose name is not shared with other instances of this mutation.
 */
class BasicCreateTableMutation(schemaName: SafeIdentifier, private val tableName: SafeIdentifier) :
    IndependentDatabaseMutation(schemaName) {

    override fun isApplied(connection: Connection): Boolean {
        return connection.with(schemaName) {
            it.query("select table_name from information_schema.tables where table_name = $tableName") { _, _ ->
                true
            }.isNotEmpty()
        }
    }

    override fun apply(connection: Connection) {
        connection.with(schemaName) {
            it.execute("create table $tableName(id int not null primary key)")
        }
    }

    override fun undo(connection: Connection) {
        connection.with(schemaName) {
            it.execute("drop table $tableName")
        }
    }
}
