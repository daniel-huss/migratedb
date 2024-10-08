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

package migratedb.v1.integrationtest.database.mutation

import migratedb.v1.integrationtest.util.base.SafeIdentifier
import migratedb.v1.integrationtest.util.base.work
import java.sql.Connection

/**
 * (SQLite only) Creates / drops a table.
 */
class SqliteCreateTableMutation(private val normalizedTable: SafeIdentifier) : IndependentDatabaseMutation {

    override fun isApplied(connection: Connection): Boolean {
        return connection.work(commit = false) {
            it.query("select name from sqlite_master where type='table' and name='$normalizedTable'") { _, _ ->
                true
            }.isNotEmpty()
        }
    }

    override fun apply(connection: Connection) {
        connection.work {
            it.execute("create table $normalizedTable(id int not null primary key)")
        }
    }

    override fun undo(connection: Connection) {
        connection.work {
            it.execute("drop table $normalizedTable")
        }
    }
}
