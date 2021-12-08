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

package migratedb.integrationtest

import migratedb.core.api.resource.LoadableResource
import migratedb.core.internal.database.base.Connection
import migratedb.core.internal.database.base.Database
import migratedb.core.internal.database.base.Schema
import migratedb.core.internal.database.base.Table
import migratedb.core.internal.jdbc.StatementInterceptor
import migratedb.core.internal.schemahistory.AppliedMigration
import migratedb.core.internal.sqlscript.SqlStatement

object NoOpIntercepter : StatementInterceptor {
    override fun init(database: Database<out Connection<*>>?, table: Table<out Database<*>, out Schema<*, *>>?) {
    }

    override fun schemaHistoryTableCreate(baseline: Boolean) {
    }

    override fun schemaHistoryTableInsert(appliedMigration: AppliedMigration?) {
    }

    override fun close() {
    }

    override fun sqlScript(resource: LoadableResource?) {
    }

    override fun sqlStatement(statement: SqlStatement?) {
    }

    override fun interceptCommand(command: String?) {
    }

    override fun interceptStatement(sql: String?) {
    }

    override fun interceptPreparedStatement(sql: String?, params: MutableMap<Int, Any>?) {
    }

    override fun interceptCallableStatement(sql: String?) {
    }

    override fun schemaHistoryTableDeleteFailed(table: Table<out Database<*>, out Schema<*, *>>?, appliedMigration: AppliedMigration?) {
    }
}
