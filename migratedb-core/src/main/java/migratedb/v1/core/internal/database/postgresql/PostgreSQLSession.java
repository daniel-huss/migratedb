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
package migratedb.v1.core.internal.database.postgresql;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.internal.database.base.BaseSession;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class PostgreSQLSession extends BaseSession {
    private final String originalRole;
    private final Configuration configuration;

    protected PostgreSQLSession(Configuration configuration, PostgreSQLDatabase database,
                                Connection connection) {
        super(database, connection);
        this.configuration = configuration;

        try {
            originalRole = jdbcTemplate.queryForString("SELECT CURRENT_USER");
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine current user", e);
        }
    }

    @Override
    protected void doRestoreOriginalState() throws SQLException {
        // Reset the role to its original value in case a migration or callback changed it
        jdbcTemplate.execute("SET ROLE '" + originalRole + "'");
    }

    @Override
    public Schema doGetCurrentSchema() throws SQLException {
        String currentSchema = jdbcTemplate.queryForString("SELECT current_schema");
        String searchPath = getCurrentSchemaNameOrSearchPath();

        if (!StringUtils.hasText(currentSchema) && !StringUtils.hasText(searchPath)) {
            throw new MigrateDbException("Unable to determine current schema as search_path is empty. " +
                                         "Set the current schema in currentSchema parameter of the JDBC URL or in " +
                                         "MigrateDB's schemas property.");
        }

        String schema = StringUtils.hasText(currentSchema) ? currentSchema : searchPath;

        return getSchema(schema);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return jdbcTemplate.queryForString("SHOW search_path");
    }

    @Override
    public void changeCurrentSchemaTo(Schema schema) {
        try {
            if (schema.getName().equals(originalSchemaNameOrSearchPath) || originalSchemaNameOrSearchPath.startsWith(
                    schema.getName() + ",") || !schema.exists()) {
                return;
            }

            if (StringUtils.hasText(originalSchemaNameOrSearchPath)) {
                doChangeCurrentSchemaOrSearchPathTo(schema + "," + originalSchemaNameOrSearchPath);
            } else {
                doChangeCurrentSchemaOrSearchPathTo(schema.toString());
            }
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error setting current schema to " + schema, e);
        }
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        jdbcTemplate.execute("SELECT set_config('search_path', ?, false)", schema);
    }

    @Override
    public PostgreSQLSchema getSchema(String name) {
        return new PostgreSQLSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public PostgreSQLDatabase getDatabase() {
        return (PostgreSQLDatabase) super.getDatabase();
    }

    @Override
    public <T> T lock(Table table, Callable<T> callable) {
        return new PostgreSQLAdvisoryLockTemplate(configuration, jdbcTemplate, table.toString().hashCode()).execute(
                callable);
    }
}
