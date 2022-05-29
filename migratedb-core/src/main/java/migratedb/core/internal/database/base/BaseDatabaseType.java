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
package migratedb.core.internal.database.base;

import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.DefaultExecutionStrategy;
import migratedb.core.internal.jdbc.JdbcUtils;
import migratedb.core.internal.jdbc.TransactionalExecutionTemplate;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.sqlscript.DefaultSqlScriptExecutor;
import migratedb.core.internal.sqlscript.ParserSqlScript;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static migratedb.core.internal.sqlscript.SqlScriptMetadataImpl.getMetadataResource;

public abstract class BaseDatabaseType implements DatabaseType {
    protected static final Log LOG = Log.getLog(BaseDatabaseType.class);

    // Don't grab semicolons and ampersands - they have special meaning in URLs
    private static final Pattern defaultJdbcCredentialsPattern = Pattern.compile("password=([^;&]*).*",
            Pattern.CASE_INSENSITIVE);

    /**
     * This is useful for databases that allow setting this in order to easily correlate individual application with
     * database connections.
     */
    protected static final String APPLICATION_NAME = "MigrateDB";

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
     * Whether this database type should handle the given JDBC url.
     */
    @Override
    public abstract boolean handlesJDBCUrl(String url);

    /**
     * When identifying database types, the priority with which this type will be used. High numbers indicate that this
     * type will be used in preference to others.
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * A regex that identifies credentials in the JDBC URL, where they conform to a pattern specific to this database.
     * The first captured group should represent the password text, so that it can be redacted if necessary.
     *
     * @return The URL regex.
     */
    @Override
    public Pattern getJDBCCredentialsPattern() {
        return defaultJdbcCredentialsPattern;
    }

    /**
     * Gets a regex that identifies credentials in the JDBC URL, where they conform to the default URL pattern. The
     * first captured group represents the password text.
     */
    public static Pattern getDefaultJDBCCredentialsPattern() {
        return defaultJdbcCredentialsPattern;
    }

    /**
     * @return The full driver class name to be instantiated to handle this url.
     */
    @Override
    public abstract String getDriverClass(String url, ClassLoader classLoader);

    /**
     * Retrieves a second choice backup driver for a JDBC url, in case the one returned by {@code getDriverClass} is not
     * available.
     *
     * @return The JDBC driver class name, {@code null} if none.
     */
    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        return null;
    }

    /**
     * This allows more fine-grained control over which DatabaseType handles which connection. MigrateDB will use the
     * first DatabaseType that returns true for this method.
     */
    @Override
    public abstract boolean handlesDatabaseProductNameAndVersion(String databaseProductName,
                                                                 String databaseProductVersion, Connection connection);

    @Override
    public Database<?> createDatabase(Configuration configuration, boolean printInfo,
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
    public abstract Database<?> createDatabase(Configuration configuration,
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
                        getMetadataResource(resourceProvider, resource),
                        mixed);
    }

    @Override
    public SqlScriptExecutorFactory createSqlScriptExecutorFactory(JdbcConnectionFactory jdbcConnectionFactory,
                                                                   CallbackExecutor callbackExecutor) {
        DatabaseType thisRef = this;
        return (connection, batch, outputQueryResults) ->
                new DefaultSqlScriptExecutor(new JdbcTemplate(connection, thisRef),
                        callbackExecutor,
                        batch,
                        outputQueryResults);
    }

    @Override
    public DatabaseExecutionStrategy createExecutionStrategy(java.sql.Connection connection) {
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

    /**
     * Set the default connection properties for this database. These can be overridden by {@code
     * setConfigConnectionProps} and {@code setOverridingConnectionProps}.
     *
     * @param url         The JDBC url.
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
    }

    /**
     * Set any necessary connection properties based on MigrateDB's configuration. These can be overridden by {@code
     * setOverridingConnectionProps}.
     *
     * @param config      The MigrateDB configuration to read properties from.
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    @Override
    public void modifyConfigConnectionProps(Configuration config, Properties props, ClassLoader classLoader) {
    }

    /**
     * These will override anything set by {@code setDefaultConnectionProps} and {@code setConfigConnectionProps} and
     * should only be used if neither of those can satisfy your requirement.
     *
     * @param props The properties to write to.
     */
    @Override
    public void modifyOverridingConnectionProps(Map<String, String> props) {
    }

    /**
     * Only applicable to embedded databases that require this.
     */
    @Override
    public void shutdownDatabase(String url, Driver driver) {
    }

    /**
     * Detects whether a user is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the user being encoded in the URL.
     */
    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return true;
    }

    /**
     * Detects whether a password is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the password being encoded in the URL.
     */
    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return true;
    }

    @Override
    public boolean externalAuthPropertiesRequired(String url, String username, String password) {
        return false;
    }

    @Override
    public Properties getExternalAuthProperties(String url, String username) {
        return new Properties();
    }

    @Override
    public void alterConnectionAsNeeded(Connection connection, Configuration configuration) {
    }

    @Override
    public String instantiateClassExtendedErrorMessage() {
        return "";
    }
}
