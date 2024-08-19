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
package migratedb.v1.core.internal.database.base;

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.DatabaseType;
import migratedb.v1.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.DefaultExecutionStrategy;
import migratedb.v1.core.internal.jdbc.JdbcUtils;
import migratedb.v1.core.internal.jdbc.TransactionalExecutionTemplate;
import migratedb.v1.core.internal.parser.BaseParser;
import migratedb.v1.core.internal.sqlscript.DefaultSqlScriptExecutor;
import migratedb.v1.core.internal.sqlscript.ParserSqlScript;
import migratedb.v1.core.internal.sqlscript.SqlScriptMetadataImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseDatabaseType implements DatabaseType {
    private static final Log LOG = Log.getLog(BaseDatabaseType.class);

    public static final int DEFAULT_PRIORITY = 0;

    /**
     * @return The human-readable name for this database.
     */
    @Override
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return The JDBC type used to represent {@code null} in prepared statements.
     */
    @Override
    public abstract int getNullType();

    /**
     * When identifying database types, the priority with which this type will be used. High numbers indicate that this
     * type will be used in preference to others.
     */
    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public Database createDatabase(Configuration configuration,
                                   boolean printInfo,
                                   JdbcConnectionFactory jdbcConnectionFactory) {
        String databaseProductName = jdbcConnectionFactory.getProductName();
        if (printInfo) {
            LOG.info("Database: " + jdbcConnectionFactory.getJdbcUrl() + " (" + databaseProductName + ")");
            LOG.debug("Driver  : " + jdbcConnectionFactory.getDriverInfo());
        }

        var database = createDatabase(configuration, jdbcConnectionFactory);

        String intendedCurrentSchema = configuration.getDefaultSchema();
        if (!database.supportsChangingCurrentSchema() && intendedCurrentSchema != null) {
            LOG.warn(databaseProductName + " does not support setting the schema for the current session. " +
                     "Default schema will NOT be changed to " + intendedCurrentSchema + " !");
        }

        return database;
    }

    @Override
    public abstract Database createDatabase(Configuration configuration,
                                            JdbcConnectionFactory jdbcConnectionFactory);

    @Override
    public abstract BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                            ParsingContext parsingContext);

    @Override
    public SqlScriptFactory createSqlScriptFactory(Configuration configuration, ParsingContext parsingContext) {
        return (resource, mixed, resourceProvider) ->
                new ParserSqlScript(createParser(configuration,
                                                 resourceProvider,
                                                 parsingContext),
                                    resource,
                                    SqlScriptMetadataImpl.getMetadataResource(resourceProvider, resource),
                                    mixed);
    }

    @Override
    public SqlScriptExecutorFactory createSqlScriptExecutorFactory(JdbcConnectionFactory jdbcConnectionFactory,
                                                                   CallbackExecutor callbackExecutor) {
        DatabaseType thisRef = this;
        return (connection, outputQueryResults) ->
                new DefaultSqlScriptExecutor(new JdbcTemplate(connection, thisRef),
                                             callbackExecutor,
                                             outputQueryResults);
    }

    @Override
    public DatabaseExecutionStrategy createExecutionStrategy(Connection connection) {
        return new DefaultExecutionStrategy();
    }

    @Override
    public ExecutionTemplate createTransactionalExecutionTemplate(Connection connection, boolean rollbackOnException) {
        return new TransactionalExecutionTemplate(connection, rollbackOnException);
    }

    /**
     * Retrieves the version string for a connection as described by SELECT VERSION(), which may differ from the
     * connection metadata.
     */
    public static String getSelectVersionOutput(Connection connection) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String result = null;

        try {
            statement = connection.prepareStatement("SELECT version()");
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                result = resultSet.getString(1);
            }
        } catch (SQLException e) {
            return "";
        } finally {
            JdbcUtils.closeResultSet(resultSet);
            JdbcUtils.closeStatement(statement);
        }

        return result;
    }

    @Override
    public void alterConnectionAsNeeded(Connection connection, Configuration configuration) {
    }
}
