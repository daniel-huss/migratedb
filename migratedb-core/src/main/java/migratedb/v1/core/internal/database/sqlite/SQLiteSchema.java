/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.database.sqlite;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation of Schema.
 */
public class SQLiteSchema extends BaseSchema {
    private static final Log LOG = Log.getLog(SQLiteSchema.class);

    private static final List<String> IGNORED_SYSTEM_TABLE_NAMES = List.of("android_metadata");

    /**
     * Creates a new SQLite schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    SQLiteSchema(JdbcTemplate jdbcTemplate, SQLiteDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        try {
            doAllTables();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    protected boolean doCheckIfEmpty() {
        for (var table : allTables()) {
            if (!IGNORED_SYSTEM_TABLE_NAMES.contains(table.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doCreate() {
        LOG.info("SQLite does not support creating schemas. Schema not created: " + name);
    }

    @Override
    protected List<SQLiteTable> doAllTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(
                "SELECT tbl_name FROM " + getDatabase().quote(name) + ".sqlite_master WHERE type='table'");

        List<SQLiteTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new SQLiteTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public SQLiteTable getTable(String tableName) {
        return new SQLiteTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected SQLiteDatabase getDatabase() {
        return (SQLiteDatabase) super.getDatabase();
    }
}
