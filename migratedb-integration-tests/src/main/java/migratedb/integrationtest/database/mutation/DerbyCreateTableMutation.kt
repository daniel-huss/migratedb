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

package migratedb.integrationtest.database.mutation

import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.work
import java.sql.Connection

/**
 * (Derby only) Creates / drops a table.
 */
class DerbyCreateTableMutation(
    private val normalizedSchema: SafeIdentifier?,
    private val normalizedTable: SafeIdentifier
) : IndependentDatabaseMutation {

    override fun isApplied(connection: Connection): Boolean {
        return connection.work(normalizedSchema, commit = false) { jdbc ->
            var query = "select * from sys.systables t"
            if (normalizedSchema != null) {
                query += " left join sys.sysschemas s on (s.schemaid = t.schemaid)"
            }
            query += " where t.tablename = '$normalizedTable'"
            if (normalizedSchema != null) {
                query += " and s.schemaname ='$normalizedSchema'"
            }
            jdbc.query(query) { _, _ -> true }.isNotEmpty()
        }
    }

    override fun apply(connection: Connection) {
        connection.work(normalizedSchema) {
            it.execute("create table $normalizedTable(id int not null primary key)")
        }
    }

    override fun undo(connection: Connection) {
        connection.work(normalizedSchema) {
            it.execute("drop table $normalizedTable")
        }
    }
}
