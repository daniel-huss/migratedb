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
package migratedb.v1.core.internal.database.db2;

import migratedb.v1.core.api.internal.database.base.DatabaseFunction;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB2 implementation of Schema.
 */
public class DB2Schema extends BaseSchema {
    /**
     * Creates a new DB2 schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    DB2Schema(JdbcTemplate jdbcTemplate, DB2Database database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT count(*) from ("
                                        + "SELECT 1 FROM syscat.schemata WHERE schemaname=?"
                                        + ")", name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return jdbcTemplate.queryForInt("select count(*) from ("
                                        + "select 1 from syscat.tables where tabschema = ? "
                                        + "union "
                                        + "select 1 from syscat.views where viewschema = ? "
                                        + "union "
                                        + "select 1 from syscat.sequences where seqschema = ? "
                                        + "union "
                                        + "select 1 from syscat.indexes where indschema = ? "
                                        + "union "
                                        + "select 1 from syscat.routines where ROUTINESCHEMA = ? "
                                        + "union "
                                        + "select 1 from syscat.triggers where trigschema = ? "
                                        + ")", name, name, name, name, name, name) == 0;
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    public List<DB2Table> findTables(String sqlQuery, String... params) throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(sqlQuery, params);
        List<DB2Table> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new DB2Table(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    protected List<DB2Table> doAllTables() throws SQLException {
        return findTables("select TABNAME from SYSCAT.TABLES where TYPE='T' and TABSCHEMA = ?", name);
    }

    @Override
    protected List<DatabaseFunction> doAllFunctions() throws SQLException {
        List<String> functionNames = jdbcTemplate.queryForStringList(
                "select SPECIFICNAME from SYSCAT.ROUTINES where"
                // Functions only
                + " ROUTINETYPE='F'"
                // That aren't system-generated or built-in
                + " AND ORIGIN IN ("
                + "'E', " // User-defined, external
                + "'M', " // Template function
                + "'Q', " // SQL-bodied
                + "'U')"  // User-defined, based on a source
                + " and ROUTINESCHEMA = ?", name);

        List<DatabaseFunction> functions = new ArrayList<>();
        for (String functionName : functionNames) {
            functions.add(getFunction(functionName));
        }

        return functions;
    }

    @Override
    public Table getTable(String tableName) {
        return new DB2Table(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    public DatabaseFunction getFunction(String functionName, String... args) {
        return new DB2DatabaseFunction(jdbcTemplate, getDatabase(), this, functionName, args);
    }

    @Override
    protected DB2Database getDatabase() {
        return (DB2Database) super.getDatabase();
    }
}
