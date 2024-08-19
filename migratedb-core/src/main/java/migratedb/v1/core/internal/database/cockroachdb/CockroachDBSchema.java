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
package migratedb.v1.core.internal.database.cockroachdb;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.internal.util.SqlCallable;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CockroachDBSchema extends BaseSchema {
    /**
     * Is this CockroachDB 1.x.
     */
    final boolean cockroachDB1;
    final boolean hasSchemaSupport;

    public CockroachDBSchema(JdbcTemplate jdbcTemplate, CockroachDBDatabase database, String name) {
        super(jdbcTemplate, database, name);
        cockroachDB1 = !database.getVersion().isAtLeast("2");
        hasSchemaSupport = getDatabase().supportsSchemas();
    }

    @Override
    protected boolean doExists() throws SQLException {
        return new CockroachDBRetryingStrategy().execute(this::doExistsOnce);
    }

    private boolean doExistsOnce() throws SQLException {
        if (hasSchemaSupport) {
            return jdbcTemplate.queryForBoolean(
                    "SELECT EXISTS ( SELECT 1 FROM information_schema.schemata WHERE schema_name=? )",
                    name);
        }
        return jdbcTemplate.queryForBoolean("SELECT EXISTS ( SELECT 1 FROM pg_database WHERE datname=? )", name);
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return new CockroachDBRetryingStrategy().execute(this::doEmptyOnce);
    }

    private boolean doEmptyOnce() throws SQLException {
        if (cockroachDB1) {
            return !jdbcTemplate.queryForBoolean("SELECT EXISTS (" +
                                                 "  SELECT 1" +
                                                 "  FROM information_schema.tables" +
                                                 "  WHERE table_schema=?" +
                                                 "  AND table_type='BASE TABLE'" +
                                                 ")", name);
        } else if (!hasSchemaSupport) {
            return !jdbcTemplate.queryForBoolean("SELECT EXISTS (" +
                                                 "  SELECT 1" +
                                                 "  FROM information_schema.tables " +
                                                 "  WHERE table_catalog=?" +
                                                 "  AND table_schema='public'" +
                                                 "  AND table_type='BASE TABLE'" +
                                                 " UNION ALL" +
                                                 "  SELECT 1" +
                                                 "  FROM information_schema.sequences " +
                                                 "  WHERE sequence_catalog=?" +
                                                 "  AND sequence_schema='public'" +
                                                 ")", name, name);
        } else {
            return !jdbcTemplate.queryForBoolean("SELECT EXISTS (" +
                                                 "  SELECT 1" +
                                                 "  FROM information_schema.tables " +
                                                 "  WHERE table_schema=?" +
                                                 "  AND table_type='BASE TABLE'" +
                                                 " UNION ALL" +
                                                 "  SELECT 1" +
                                                 "  FROM information_schema.sequences " +
                                                 "  WHERE sequence_schema=?" +
                                                 ")", name, name);
        }
    }

    @Override
    protected void doCreate() throws SQLException {
        new CockroachDBRetryingStrategy().execute((SqlCallable<Integer>) () -> {
            doCreateOnce();
            return null;
        });
    }

    protected void doCreateOnce() throws SQLException {
        if (hasSchemaSupport) {
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + getDatabase().quote(name));
        } else {
            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS " + getDatabase().quote(name));
        }
    }

    @Override
    protected List<CockroachDBTable> doAllTables() throws SQLException {
        String query;
        if (cockroachDB1 || hasSchemaSupport) {
            query =
                    //Search for all the table names
                    "SELECT table_name FROM information_schema.tables" +
                    //in this schema
                    " WHERE table_schema=?" +
                    //that are real tables (as opposed to views)
                    " AND table_type='BASE TABLE'";
        } else {
            query =
                    //Search for all the table names
                    "SELECT table_name FROM information_schema.tables" +
                    //in this database
                    " WHERE table_catalog=?" +
                    " AND table_schema='public'" +
                    //that are real tables (as opposed to views)
                    " AND table_type='BASE TABLE'";
        }

        List<String> tableNames = jdbcTemplate.queryForStringList(query, name);
        //Views and child tables are excluded as they are dropped with the parent table when using cascade.

        List<CockroachDBTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new CockroachDBTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new CockroachDBTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected CockroachDBDatabase getDatabase() {
        return (CockroachDBDatabase) super.getDatabase();
    }
}
