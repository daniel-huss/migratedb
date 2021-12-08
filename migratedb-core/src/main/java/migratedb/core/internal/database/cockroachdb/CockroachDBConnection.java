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
package migratedb.core.internal.database.cockroachdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.base.Connection;
import migratedb.core.internal.database.base.Schema;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.util.StringUtils;

public class CockroachDBConnection extends Connection<CockroachDBDatabase> {
    private static final Log LOG = Log.getLog(CockroachDBConnection.class);

    public CockroachDBConnection(CockroachDBDatabase database, java.sql.Connection connection) {
        super(database, connection);
    }

    @Override
    public Schema getSchema(String name) {
        return new CockroachDBSchema(jdbcTemplate, database, name);
    }

    @Override
    public Schema doGetCurrentSchema() throws SQLException {
        if (database.supportsSchemas()) {
            String currentSchema = jdbcTemplate.queryForString("SELECT current_schema");
            if (StringUtils.hasText(currentSchema)) {
                return getSchema(currentSchema);
            }

            String searchPath = getCurrentSchemaNameOrSearchPath();
            if (!StringUtils.hasText(searchPath)) {
                throw new MigrateDbException(
                    "Unable to determine current schema as search_path is empty. Set the current schema in " +
                    "currentSchema parameter of the JDBC URL or in MigrateDb's schemas property.");
            }
        }
        return super.doGetCurrentSchema();
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        if (database.supportsSchemas()) {
            String sp = jdbcTemplate.queryForString("SHOW search_path");
            // Up to Cockroach 20, the default response is "public". In 21, that became "$user,public", but this is
            // illegal in the corresponding SET query. Normally this simply results in an exception which we skip over,
            // but in dry runs the produced script will be invalid and error when you run it.
            if (sp.contains("$user")) {
                LOG.warn("Search path contains $user; removing...");
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
        if (database.supportsSchemas()) {
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
}
