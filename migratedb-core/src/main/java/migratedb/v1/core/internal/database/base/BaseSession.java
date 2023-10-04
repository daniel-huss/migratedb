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
package migratedb.v1.core.internal.database.base;

import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.jdbc.ExecutionTemplateFactory;
import migratedb.v1.core.internal.jdbc.JdbcUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public abstract class BaseSession implements Session {
    protected JdbcTemplate jdbcTemplate;

    private final Database database;
    private final Connection jdbcConnection;
    private final boolean originalAutoCommit;

    protected final String originalSchemaNameOrSearchPath;

    protected BaseSession(Database database, Connection connection) {
        this.database = database;

        try {
            this.originalAutoCommit = connection.getAutoCommit();
            if (!originalAutoCommit) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to turn on auto-commit for the connection", e);
        }

        this.jdbcConnection = connection;
        jdbcTemplate = new JdbcTemplate(jdbcConnection, database.getDatabaseType());
        try {
            originalSchemaNameOrSearchPath = getCurrentSchemaNameOrSearchPath();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine the original schema for the connection", e);
        }
    }

    /**
     * @throws SQLException when the current schema name or search path could not be retrieved.
     */
    protected abstract String getCurrentSchemaNameOrSearchPath() throws SQLException;

    @Override
    public Schema getCurrentSchema() {
        try {
            return doGetCurrentSchema();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine the current schema for the connection", e);
        }
    }

    protected Schema doGetCurrentSchema() throws SQLException {
        return getSchema(getCurrentSchemaNameOrSearchPath());
    }

    @Override
    public void changeCurrentSchemaTo(Schema schema) {
        try {
            if (!schema.exists()) {
                return;
            }
            doChangeCurrentSchemaOrSearchPathTo(schema.getName());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error setting current schema to " + schema, e);
        }
    }

    /**
     * @param schemaNameOrSearchPath The new current schema for this connection.
     * @throws SQLException when the current schema could not be set.
     */
    protected void doChangeCurrentSchemaOrSearchPathTo(String schemaNameOrSearchPath) throws SQLException {
    }

    @Override
    public <T> T lock(Table table, Callable<T> callable) {
        return ExecutionTemplateFactory
                .createTableExclusiveExecutionTemplate(jdbcTemplate.getConnection(), table, database)
                .execute(callable);
    }

    @Override
    public final JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public final void close() {
        try {
            restoreOriginalState();
            restoreOriginalSchema();
            restoreOriginalAutoCommit();
        } finally {
            JdbcUtils.closeConnection(jdbcConnection);
        }
    }

    private void restoreOriginalSchema() {
        ExecutionTemplateFactory.createExecutionTemplate(jdbcConnection, database).execute((Callable<Void>) () -> {
            try {
                doChangeCurrentSchemaOrSearchPathTo(originalSchemaNameOrSearchPath);
            } catch (SQLException e) {
                throw new MigrateDbSqlException("Unable to restore original schema", e);
            }
            return null;
        });
    }

    @Override
    public final void restoreOriginalState() {
        try {
            doRestoreOriginalState();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to restore connection to its original state", e);
        }
    }

    private void restoreOriginalAutoCommit() {
        try {
            jdbcConnection.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to restore connection to its original auto-commit setting", e);
        }
    }

    protected void doRestoreOriginalState() throws SQLException {
    }

    @Override
    public final Connection getJdbcConnection() {
        return jdbcConnection;
    }

    public Database getDatabase() {
        return database;
    }
}
