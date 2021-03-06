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
package migratedb.core.internal.jdbc;

import migratedb.core.api.DatabaseTypeRegister;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.internal.util.ClassUtils;
import migratedb.core.internal.util.StringUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * YAGNI: The simplest DataSource implementation that works for MigrateDB .
 */
public class DriverDataSource implements DataSource {
    private Driver driver;
    private final String url;
    private final DatabaseType type;
    private final String user;
    private final String password;
    private final Properties defaultProperties;
    private final Map<String, String> additionalProperties;
    private final DatabaseTypeRegister databaseTypeRegister;
    private boolean autoCommit = true;

    public DriverDataSource(ClassLoader classLoader,
                            String driverClass,
                            String url,
                            String user,
                            String password,
                            DatabaseTypeRegister databaseTypeRegister)
            throws MigrateDbException {
        this(classLoader,
                driverClass,
                url,
                user,
                password,
                null,
                new Properties(),
                new HashMap<>(),
                databaseTypeRegister);
    }

    public DriverDataSource(ClassLoader classLoader,
                            String driverClass,
                            String url,
                            String user,
                            String password,
                            Configuration configuration,
                            DatabaseTypeRegister databaseTypeRegister)
            throws MigrateDbException {
        this(classLoader,
                driverClass,
                url,
                user,
                password,
                configuration,
                new Properties(),
                configuration.getJdbcProperties(),
                databaseTypeRegister);
    }

    public DriverDataSource(ClassLoader classLoader,
                            String driverClass,
                            String url,
                            String user,
                            String password,
                            Map<String, String> additionalProperties,
                            DatabaseTypeRegister databaseTypeRegister)
            throws MigrateDbException {
        this(classLoader,
                driverClass,
                url,
                user,
                password,
                null,
                new Properties(),
                additionalProperties,
                databaseTypeRegister);
    }

    public DriverDataSource(ClassLoader classLoader,
                            String driverClass,
                            String url,
                            String user,
                            String password,
                            Configuration configuration,
                            Map<String, String> additionalProperties,
                            DatabaseTypeRegister databaseTypeRegister) throws MigrateDbException {
        this(classLoader,
                driverClass,
                url,
                user,
                password,
                configuration,
                new Properties(),
                additionalProperties,
                databaseTypeRegister);
    }

    /**
     * Creates a new DriverDataSource.
     *
     * @param classLoader          The ClassLoader to use.
     * @param driverClass          The name of the JDBC Driver class to use. {@code null} for url-based autodetection.
     * @param url                  The JDBC URL to use for connecting through the Driver. (required)
     * @param user                 The JDBC user to use for connecting through the Driver.
     * @param password             The JDBC password to use for connecting through the Driver.
     * @param configuration        The MigrateDB configuration
     * @param defaultProperties    Default values of properties to pass to the connection (can be overridden by {@code
     *                             additionalProperties})
     * @param additionalProperties The properties to pass to the connection.
     * @throws MigrateDbException when the datasource could not be created.
     */
    public DriverDataSource(ClassLoader classLoader, String driverClass, String url, String user, String password,
                            Configuration configuration, Properties defaultProperties,
                            Map<String, String> additionalProperties,
                            DatabaseTypeRegister databaseTypeRegister) throws MigrateDbException {
        this.databaseTypeRegister = databaseTypeRegister;
        this.url = detectFallbackUrl(url);

        this.type = databaseTypeRegister.getDatabaseTypeForUrl(url);

        if (!StringUtils.hasLength(driverClass)) {
            if (type == null) {
                throw new MigrateDbException(
                        "Unable to autodetect JDBC driver for url: " + databaseTypeRegister.redactJdbcUrl(url));
            }

            driverClass = type.getDriverClass(url, classLoader);
        }

        this.additionalProperties = Objects.requireNonNullElseGet(additionalProperties, HashMap::new);
        this.defaultProperties = new Properties(defaultProperties);
        type.modifyDefaultConnectionProps(url, defaultProperties, classLoader);
        type.modifyConfigConnectionProps(configuration, defaultProperties, classLoader);
        type.modifyOverridingConnectionProps(this.additionalProperties);

        try {
            this.driver = ClassUtils.instantiate(driverClass, classLoader);
        } catch (MigrateDbException e) {
            String backupDriverClass = type.getBackupDriverClass(url, classLoader);
            if (backupDriverClass == null) {
                String extendedError = type.instantiateClassExtendedErrorMessage();
                if (StringUtils.hasText(extendedError)) {
                    extendedError = "\n" + extendedError;
                }
                throw new MigrateDbException("Unable to instantiate JDBC driver: " + driverClass
                        + " => Check whether the jar file is present"
                        + extendedError, e,
                        ErrorCode.JDBC_DRIVER);
            }
            try {
                this.driver = ClassUtils.instantiate(backupDriverClass, classLoader);
            } catch (RuntimeException e1) {
                // Only report original exception about primary driver
                throw new MigrateDbException(
                        "Unable to instantiate JDBC driver: " + driverClass + " or backup driver: " + backupDriverClass +
                                " => Check whether the jar file is present", e,
                        ErrorCode.JDBC_DRIVER);
            }
        }

        this.user = detectFallbackUser(user);
        this.password = detectFallbackPassword(password);

        if (type.externalAuthPropertiesRequired(url, user, password)) {
            defaultProperties.putAll(type.getExternalAuthProperties(url, user));
        }
    }

    /**
     * Detects a fallback url in case this one is missing.
     *
     * @param url The url to check.
     * @return The url to use.
     */
    private String detectFallbackUrl(String url) {
        if (!StringUtils.hasText(url)) {
            // Attempt fallback to the automatically provided Boxfuse database URL (https://boxfuse.com/docs/databases#envvars)
            String boxfuseDatabaseUrl = System.getenv("BOXFUSE_DATABASE_URL");
            if (StringUtils.hasText(boxfuseDatabaseUrl)) {
                return boxfuseDatabaseUrl;
            }

            throw new MigrateDbException("Missing required JDBC URL. Unable to create DataSource!");
        }

        return url;
    }

    /**
     * Detects a fallback user in case this one is missing.
     *
     * @param user The user to check.
     * @return The user to use.
     */
    private String detectFallbackUser(String user) {
        if (!StringUtils.hasText(user)) {
            // Attempt fallback to the automatically provided Boxfuse database user (https://boxfuse.com/docs/databases#envvars)
            String boxfuseDatabaseUser = System.getenv("BOXFUSE_DATABASE_USER");
            if (StringUtils.hasText(boxfuseDatabaseUser)) {
                return boxfuseDatabaseUser;
            }
        }
        return user;
    }

    /**
     * Detects a fallback password in case this one is missing.
     *
     * @param password The password to check.
     * @return The password to use.
     */
    private String detectFallbackPassword(String password) {
        if (!StringUtils.hasText(password)) {
            // Attempt fallback to the automatically provided Boxfuse database password (https://boxfuse.com/docs/databases#envvars)
            String boxfuseDatabasePassword = System.getenv("BOXFUSE_DATABASE_PASSWORD");
            if (StringUtils.hasText(boxfuseDatabasePassword)) {
                return boxfuseDatabasePassword;
            }
        }
        return password;
    }

    /**
     * @return the JDBC Driver instance to use.
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * @return the JDBC URL to use for connecting through the Driver.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the JDBC user to use for connecting through the Driver.
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the JDBC password to use for connecting through the Driver.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return The additional properties to pass to a JDBC connection.
     */
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * This implementation delegates to {@code getConnectionFromDriver}, using the default user and password of this
     * DataSource.
     *
     * @see #getConnectionFromDriver(String, String)
     */
    @Override
    public Connection getConnection() throws SQLException {
        return getConnectionFromDriver(getUser(), getPassword());
    }

    /**
     * This implementation delegates to {@code getConnectionFromDriver}, using the given user and password.
     *
     * @see #getConnectionFromDriver(String, String)
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnectionFromDriver(username, password);
    }

    /**
     * Build properties for the Driver, including the given user and password (if any), and obtain a corresponding
     * Connection.
     *
     * @param username the name of the user
     * @param password the password to use
     * @return the obtained Connection
     * @throws SQLException in case of failure
     * @see java.sql.Driver#connect(String, java.util.Properties)
     */
    protected Connection getConnectionFromDriver(String username, String password) throws SQLException {
        Properties properties = new Properties(defaultProperties);
        properties.putAll(additionalProperties);

        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }

        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new MigrateDbException("Unable to connect to " + databaseTypeRegister.redactJdbcUrl(url));
        }
        connection.setAutoCommit(autoCommit);
        return connection;
    }

    /**
     * @return Whether connection should have auto commit activated or not. Default: {@code true}
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * @param autoCommit Whether connection should have auto commit activated or not. Default: {@code true}
     */
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void shutdownDatabase() {
        type.shutdownDatabase(url, driver);
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public void setLoginTimeout(int timeout) {
        unsupportedMethod("setLoginTimeout");
    }

    @Override
    public PrintWriter getLogWriter() {
        unsupportedMethod("getLogWriter");
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter pw) {
        unsupportedMethod("setLogWriter");
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        unsupportedMethod("unwrap");
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return DataSource.class.equals(iface);
    }

    @Override
    public Logger getParentLogger() {
        unsupportedMethod("getParentLogger");
        return null;
    }

    private void unsupportedMethod(String methodName) {
        throw new UnsupportedOperationException(methodName);
    }
}
