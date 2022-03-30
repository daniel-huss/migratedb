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
package migratedb.core.api.internal.jdbc;

import java.util.Map;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.internal.sqlscript.SqlStatement;
import migratedb.core.api.resource.Resource;

public interface StatementInterceptor {
    void init(Database database, Table table);

    void schemaHistoryTableCreate(boolean baseline);

    void schemaHistoryTableInsert(AppliedMigration appliedMigration);

    void close();

    void sqlScript(Resource resource);

    void sqlStatement(SqlStatement statement);

    void interceptCommand(String command);

    void interceptStatement(String sql);

    void interceptPreparedStatement(String sql, Map<Integer, Object> params);

    void interceptCallableStatement(String sql);

    void schemaHistoryTableDeleteFailed(Table table, AppliedMigration appliedMigration);

    static StatementInterceptor doNothing() {
        return new StatementInterceptor() {
            @Override
            public void init(Database database, Table table) {

            }

            @Override
            public void schemaHistoryTableCreate(boolean baseline) {

            }

            @Override
            public void schemaHistoryTableInsert(AppliedMigration appliedMigration) {

            }

            @Override
            public void close() {

            }

            @Override
            public void sqlScript(Resource resource) {

            }

            @Override
            public void sqlStatement(SqlStatement statement) {

            }

            @Override
            public void interceptCommand(String command) {

            }

            @Override
            public void interceptStatement(String sql) {

            }

            @Override
            public void interceptPreparedStatement(String sql, Map<Integer, Object> params) {

            }

            @Override
            public void interceptCallableStatement(String sql) {

            }

            @Override
            public void schemaHistoryTableDeleteFailed(Table table, AppliedMigration appliedMigration) {

            }
        };
    }
}
