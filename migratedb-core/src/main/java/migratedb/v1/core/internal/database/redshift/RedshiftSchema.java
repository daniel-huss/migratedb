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
package migratedb.v1.core.internal.database.redshift;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL implementation of Schema.
 */
public class RedshiftSchema extends BaseSchema {
    /**
     * Creates a new PostgreSQL schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    RedshiftSchema(JdbcTemplate jdbcTemplate, RedshiftDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM pg_namespace WHERE nspname=?", name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return !jdbcTemplate.queryForBoolean("SELECT EXISTS (   SELECT 1\n" +
                                             "   FROM   pg_catalog.pg_class c\n" +
                                             "   JOIN   pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" +
                                             "   WHERE  n.nspname = ?)", name);
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    @Override
    protected List<RedshiftTable> doAllTables() throws SQLException {
        List<String> tableNames =
                jdbcTemplate.queryForStringList(
                        //Search for all the table names
                        "SELECT t.table_name FROM information_schema.tables t" +
                        //in this schema
                        " WHERE table_schema=?" +
                        //that are real tables (as opposed to views)
                        " AND table_type='BASE TABLE'",
                        name
                );
        //Views and child tables are excluded as they are dropped with the parent table when using cascade.

        List<RedshiftTable> tables = new ArrayList<>(tableNames.size());
        for (String tableName : tableNames) {
            tables.add(new RedshiftTable(jdbcTemplate, database(), this, tableName));
        }
        return tables;
    }

    @Override
    public RedshiftTable getTable(String tableName) {
        return new RedshiftTable(jdbcTemplate, database(), this, tableName);
    }

    private RedshiftDatabase database() {
        return (RedshiftDatabase) super.getDatabase();
    }
}
