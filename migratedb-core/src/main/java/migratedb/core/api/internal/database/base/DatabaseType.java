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
package migratedb.core.api.internal.database.base;

import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.parser.Parser;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Extension point for supported database types. Instances are unmodifiable.
 * FIXME oh what the shit, why are there mutator methods like setConfigConnectionProps?!
 */
public interface DatabaseType {
    /**
     * @return The human-readable name for this database type.
     */
    String getName();

    /**
     * @return The JDBC type used to represent {@code null} in prepared statements.
     */
    int getNullType();

    /**
     * Check if this database type should handle the given JDBC url
     *
     * @param url The JDBC url.
     * @return {@code true} if this handles the JDBC url, {@code false} if not.
     */
    boolean handlesJDBCUrl(String url);

    /**
     * When identifying database types, the priority with which this type will be used. High numbers indicate that this
     * type will be used in preference to others.
     */
    int getPriority();

    /**
     * A regex that identifies credentials in the JDBC URL, where they conform to a pattern specific to this database.
     * The first captured group should represent the password text, so that it can be redacted if necessary.
     *
     * @return The URL regex.
     */
    Pattern getJDBCCredentialsPattern();

    /**
     * Get the driver class used to handle this JDBC url. This will only be called if {@code matchesJDBCUrl} previously
     * returned {@code true}.
     *
     * @param url         The JDBC url.
     * @param classLoader The classLoader to check for driver classes.
     * @return The full driver class name to be instantiated to handle this url.
     */
    String getDriverClass(String url, ClassLoader classLoader);

    /**
     * Retrieves a second choice backup driver for a JDBC url, in case the one returned by {@code getDriverClass} is not
     * available.
     *
     * @param url         The JDBC url.
     * @param classLoader The classLoader to check for driver classes.
     * @return The JDBC driver. {@code null} if none.
     */
    String getBackupDriverClass(String url, ClassLoader classLoader);

    /**
     * Check if this database type handles the connection product name and version. This allows more fine-grained
     * control over which DatabaseType handles which connection. MigrateDb will use the first DatabaseType that returns
     * true for this method.
     *
     * @param databaseProductName    The product name returned by the database.
     * @param databaseProductVersion The product version returned by the database.
     * @param connection             The connection used to connect to the database.
     * @return {@code true} if this handles the product name and version, {@code false} if not.
     */
    boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                 Connection connection);

    /**
     * Initializes the Database class, and optionally prints some information.
     *
     * @param configuration         The MigrateDb configuration.
     * @param jdbcConnectionFactory The current connection factory.
     * @param printInfo             Where the DB info should be printed in the logs.
     * @return The appropriate Database class.
     */
    Database<?> createDatabase(Configuration configuration,
                               boolean printInfo,
                               JdbcConnectionFactory jdbcConnectionFactory);

    /**
     * Initializes the Database used by this Database Type.
     *
     * @param configuration         The MigrateDb configuration.
     * @param jdbcConnectionFactory The current connection factory.
     * @return The Database.
     */
    Database<?> createDatabase(Configuration configuration,
                               JdbcConnectionFactory jdbcConnectionFactory);

    /**
     * Initializes the Parser used by this Database Type.
     *
     * @param configuration The MigrateDb configuration.
     * @return The Parser.
     */
    Parser createParser(Configuration configuration, ResourceProvider resourceProvider, ParsingContext parsingContext);

    /**
     * Initializes the SqlScriptFactory used by this Database Type.
     *
     * @param configuration The MigrateDb configuration.
     * @return The SqlScriptFactory.
     */
    SqlScriptFactory createSqlScriptFactory(Configuration configuration,
                                            ParsingContext parsingContext);

    /**
     * Initializes the SqlScriptExecutorFactory used by this Database Type.
     *
     * @param jdbcConnectionFactory The current connection factory.
     * @return The SqlScriptExecutorFactory.
     */
    SqlScriptExecutorFactory createSqlScriptExecutorFactory(JdbcConnectionFactory jdbcConnectionFactory,
                                                            CallbackExecutor callbackExecutor);

    /**
     * Initializes the DatabaseExecutionStrategy used by this Database Type.
     *
     * @return The DatabaseExecutionStrategy.
     */
    DatabaseExecutionStrategy createExecutionStrategy(java.sql.Connection connection);

    /**
     * Initializes the ExecutionTemplate used by this Database Type.
     *
     * @return The ExecutionTemplate.
     */
    ExecutionTemplate createTransactionalExecutionTemplate(Connection connection, boolean rollbackOnException);

    /**
     * Set the default connection properties for this database. These can be overridden by {@code
     * setConfigConnectionProps} and {@code setOverridingConnectionProps}
     *
     * @param url         The JDBC url.
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader);

    /**
     * Set any necessary connection properties based on MigrateDb's configuration. These can be overridden by {@code
     * setOverridingConnectionProps}
     *
     * @param config      The MigrateDb configuration to read properties from
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    void setConfigConnectionProps(Configuration config, Properties props, ClassLoader classLoader);

    /**
     * Set any overriding connection properties. These will override anything set by {@code setDefaultConnectionProps}
     * and {@code setConfigConnectionProps} and should only be used if neither of those can satisfy your requirement.
     *
     * @param props The properties to write to.
     */
    void setOverridingConnectionProps(Map<String, String> props);

    /**
     * Shutdown the database that was opened (only applicable to embedded databases that require this).
     *
     * @param url    The JDBC url used to create the database.
     * @param driver The driver created for the url.
     */
    void shutdownDatabase(String url, Driver driver);

    /**
     * Detects whether a user is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the user being encoded in the URL
     *
     * @param url The url to check
     * @return true if a username needs to be provided
     */
    boolean detectUserRequiredByUrl(String url);

    /**
     * Detects whether a password is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the password being encoded in the URL
     *
     * @param url The url to check
     * @return true if a password needs to be provided
     */
    boolean detectPasswordRequiredByUrl(String url);

    /**
     * Detects whether or not external authentication is required.
     *
     * @return true if external authentication is required, else false.
     */
    boolean externalAuthPropertiesRequired(String url, String username, String password);

    /**
     * @param url      The JDBC url.
     * @param username The username for the connection.
     * @return Authentication properties from database specific locations (e.g. pgpass)
     */
    Properties getExternalAuthProperties(String url, String username);

    /**
     * Carries out any manipulation on the Connection that is required by MigrateDb's config
     *
     * @param connection    The JDBC connection.
     * @param configuration The MigrateDb configuration.
     */
    Connection alterConnectionAsNeeded(Connection connection, Configuration configuration);

    /**
     * @return A hint on the requirements for creating database instances (libs on class path, etc.)
     */
    String instantiateClassExtendedErrorMessage();
}
