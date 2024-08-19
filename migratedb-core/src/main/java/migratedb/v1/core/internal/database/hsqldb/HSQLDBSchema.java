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
package migratedb.v1.core.internal.database.hsqldb;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * HSQLDB implementation of Schema.
 */
public class HSQLDBSchema extends BaseSchema {
    /**
     * Creates a new Hsql schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    HSQLDBSchema(JdbcTemplate jdbcTemplate, HSQLDBDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT (*) FROM information_schema.system_schemas WHERE table_schem=?",
                                        name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() {
        return allTables().isEmpty();
    }

    @Override
    protected void doCreate() throws SQLException {
        String user = jdbcTemplate.queryForString("SELECT USER() FROM (VALUES(0))");
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name) + " AUTHORIZATION " + user);
    }

    @Override
    protected List<HSQLDBTable> doAllTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_TABLES where TABLE_SCHEM = ? AND TABLE_TYPE = 'TABLE'",
                name);

        List<HSQLDBTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new HSQLDBTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    protected HSQLDBDatabase getDatabase() {
        return (HSQLDBDatabase) super.getDatabase();
    }

    @Override
    public Table getTable(String tableName) {
        return new HSQLDBTable(jdbcTemplate, getDatabase(), this, tableName);
    }
}
