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
package migratedb.v1.core.internal.database.postgresql;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL implementation of Schema.
 */
public class PostgreSQLSchema extends BaseSchema {
    /**
     * Creates a new PostgreSQL schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    protected PostgreSQLSchema(JdbcTemplate jdbcTemplate, PostgreSQLDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM pg_namespace WHERE nspname=?", name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return !jdbcTemplate.queryForBoolean("SELECT EXISTS (\n" +
                                             "    SELECT c.oid FROM pg_catalog.pg_class c\n" +
                                             "    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" +
                                             "    LEFT JOIN pg_catalog.pg_depend d ON d.objid = c.oid AND d.deptype =" +
                                             " 'e'\n" +
                                             "    WHERE  n.nspname = ? AND d.objid IS NULL AND c.relkind IN ('r', " +
                                             "'v', 'S', 't')\n" +
                                             "  UNION ALL\n" +
                                             "    SELECT t.oid FROM pg_catalog.pg_type t\n" +
                                             "    JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace\n" +
                                             "    LEFT JOIN pg_catalog.pg_depend d ON d.objid = t.oid AND d.deptype =" +
                                             " 'e'\n" +
                                             "    WHERE n.nspname = ? AND d.objid IS NULL AND t.typcategory NOT IN " +
                                             "('A', 'C')\n" +
                                             "  UNION ALL\n" +
                                             "    SELECT p.oid FROM pg_catalog.pg_proc p\n" +
                                             "    JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace\n" +
                                             "    LEFT JOIN pg_catalog.pg_depend d ON d.objid = p.oid AND d.deptype =" +
                                             " 'e'\n" +
                                             "    WHERE n.nspname = ? AND d.objid IS NULL\n" +
                                             ")", name, name, name);
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    @Override
    protected List<PostgreSQLTable> doAllTables() throws SQLException {
        List<String> tableNames =
                jdbcTemplate.queryForStringList(
                        //Search for all the table names
                        "SELECT t.table_name FROM information_schema.tables t" +
                        // that don't depend on an extension
                        " LEFT JOIN pg_depend dep ON dep.objid = (quote_ident(t.table_schema)||'.'||quote_ident(t.table_name)" +
                        ")::regclass::oid AND dep.deptype = 'e'" +
                        // in this schema
                        " WHERE table_schema=?" +
                        //that are real tables (as opposed to views)
                        " AND table_type='BASE TABLE'" +
                        // with no extension depending on them
                        " AND dep.objid IS NULL" +
                        // and are not child tables (= do not inherit from another table).
                        " AND NOT (SELECT EXISTS (SELECT inhrelid FROM pg_catalog.pg_inherits" +
                        " WHERE inhrelid = (quote_ident(t.table_schema)||'.'||quote_ident(t.table_name))::regclass::oid))",
                        name
                );

        List<PostgreSQLTable> tables = new ArrayList<>(tableNames.size());
        for (String tableName : tableNames) {
            tables.add(new PostgreSQLTable(jdbcTemplate, database(), this, tableName));
        }
        return tables;
    }

    @Override
    public PostgreSQLTable getTable(String tableName) {
        return new PostgreSQLTable(jdbcTemplate, database(), this, tableName);
    }

    private PostgreSQLDatabase database() {
        return (PostgreSQLDatabase) super.getDatabase();
    }
}
