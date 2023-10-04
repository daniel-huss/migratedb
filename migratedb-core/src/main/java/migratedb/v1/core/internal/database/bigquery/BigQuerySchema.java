/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.database.bigquery;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BigQuerySchema extends BaseSchema {
    BigQuerySchema(JdbcTemplate jdbcTemplate, BigQueryDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        /*
         * We have to provide region to query INFORMATION_SCHEMA.SCHEMATA correctly.
         * Otherwise, it defaults to US.
         * So we make a workaround to query the schema.INFORMATION_SCHEMA.TABLES view.
         */
        try {
            return jdbcTemplate.queryForInt(
                    "SELECT COUNT(table_name) FROM " + getDatabase().quote(name) + ".INFORMATION_SCHEMA.TABLES") >= 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        // The TABLES table contains one record for each table, view, materialized view, and external table.
        return doExists() &&
               (jdbcTemplate.queryForInt(
                       "SELECT COUNT(table_name) FROM " + getDatabase().quote(name) + ".INFORMATION_SCHEMA.TABLES")
                + jdbcTemplate.queryForInt(
                       "SELECT COUNT(routine_name) FROM " + getDatabase().quote(name) + ".INFORMATION_SCHEMA.ROUTINES")
                == 0);
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + getDatabase().quote(name));
    }

    @Override
    protected List<BigQueryTable> doAllTables() throws SQLException {
        // Search for all the table names
        List<String> tableNames =
                jdbcTemplate.queryForStringList(
                        "SELECT table_name FROM " + getDatabase().quote(name) +
                        ".INFORMATION_SCHEMA.TABLES WHERE table_type='BASE TABLE'"
                );
        List<BigQueryTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new BigQueryTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new BigQueryTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected BigQueryDatabase getDatabase() {
        return (BigQueryDatabase) super.getDatabase();
    }
}
