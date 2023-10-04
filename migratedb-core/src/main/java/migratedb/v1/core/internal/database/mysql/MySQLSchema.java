/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.database.mysql;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MySQLSchema extends BaseSchema {

    MySQLSchema(JdbcTemplate jdbcTemplate, MySQLDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(1) FROM information_schema.schemata WHERE schema_name=? LIMIT 1",
                                        name) > 0;
    }

    @Override
    protected MySQLDatabase getDatabase() {
        return (MySQLDatabase) super.getDatabase();
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        List<String> params = new ArrayList<>(Arrays.asList(name, name, name, name, name));
        if (getDatabase().eventSchedulerQueryable) {
            params.add(name);
        }

        return jdbcTemplate.queryForInt("SELECT SUM(found) FROM ("
                                        +
                                        "(SELECT 1 as found FROM information_schema.tables WHERE table_schema=?) " +
                                        "UNION ALL "
                                        +
                                        "(SELECT 1 as found FROM information_schema.views WHERE table_schema=? LIMIT " +
                                        "1) UNION ALL "
                                        +
                                        "(SELECT 1 as found FROM information_schema.table_constraints WHERE " +
                                        "table_schema=? LIMIT 1) UNION ALL "
                                        +
                                        "(SELECT 1 as found FROM information_schema.triggers WHERE " +
                                        "event_object_schema=?  LIMIT 1) UNION ALL "
                                        +
                                        "(SELECT 1 as found FROM information_schema.routines WHERE routine_schema=? " +
                                        "LIMIT 1)"
                                        // #2410 Unlike MySQL, MariaDB 10.0 and newer don't allow the events table to
                                        // be queried
                                        // when the event scheduled is DISABLED or in some rare cases OFF
                                        + (getDatabase().eventSchedulerQueryable
                                                ? " UNION ALL (SELECT 1 as found FROM information_schema.events WHERE " +
                                                  "event_schema=? LIMIT 1)"
                                                : "")
                                        + ") as all_found",
                                        params.toArray(new String[0])
        ) == 0;
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    @Override
    protected List<MySQLTable> doAllTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=?" +
                " AND table_type IN ('BASE TABLE', 'SYSTEM VERSIONED')", name);

        List<MySQLTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new MySQLTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new MySQLTable(jdbcTemplate, getDatabase(), this, tableName);
    }
}
