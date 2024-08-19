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

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseSession;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class CockroachDBSession extends BaseSession {
    private static final Log LOG = Log.getLog(CockroachDBSession.class);

    public CockroachDBSession(CockroachDBDatabase database, Connection connection) {
        super(database, connection);
    }

    @Override
    public CockroachDBSchema getSchema(String name) {
        return new CockroachDBSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public Schema doGetCurrentSchema() throws SQLException {
        if (getDatabase().supportsSchemas()) {
            String currentSchema = jdbcTemplate.queryForString("SELECT current_schema");
            if (StringUtils.hasText(currentSchema)) {
                return getSchema(currentSchema);
            }

            String searchPath = getCurrentSchemaNameOrSearchPath();
            if (!StringUtils.hasText(searchPath)) {
                throw new MigrateDbException(
                        "Unable to determine current schema as search_path is empty. Set the current schema in " +
                        "currentSchema parameter of the JDBC URL or in MigrateDB's schemas property.");
            }
        }
        return super.doGetCurrentSchema();
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        if (getDatabase().supportsSchemas()) {
            String sp = jdbcTemplate.queryForString("SHOW search_path");
            // Up to Cockroach 20, the default response is "public". In 21, that became "$user,public", but this is
            // illegal in the corresponding SET query.
            if (sp.contains("$user")) {
                LOG.debug("Search path contains $user; removing...");
                ArrayList<String> paths = new ArrayList<>(Arrays.asList(sp.split(",")));
                paths.remove("$user");
                sp = String.join(",", paths);
            }
            return sp;
        } else {
            return jdbcTemplate.queryForString("SHOW database");
        }
    }

    @Override
    public void changeCurrentSchemaTo(Schema schema) {
        try {
            // Avoid unnecessary schema changes as this trips up CockroachDB
            if (schema.getName().equals(originalSchemaNameOrSearchPath) || !schema.exists()) {
                return;
            }
            doChangeCurrentSchemaOrSearchPathTo(schema.getName());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error setting current schema to " + schema, e);
        }
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        if (getDatabase().supportsSchemas()) {
            if (!StringUtils.hasLength(schema)) {
                schema = "public";
            }
            jdbcTemplate.execute("SET search_path = " + schema);
        } else {
            if (!StringUtils.hasLength(schema)) {
                schema = "DEFAULT";
            }
            jdbcTemplate.execute("SET database = " + schema);
        }
    }

    @Override
    public CockroachDBDatabase getDatabase() {
        return (CockroachDBDatabase) super.getDatabase();
    }
}
