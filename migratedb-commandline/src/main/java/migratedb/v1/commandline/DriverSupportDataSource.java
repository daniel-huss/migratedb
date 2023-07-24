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
package migratedb.v1.commandline;

import migratedb.v1.core.api.ConnectionProvider;
import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.internal.util.ClassUtils;
import migratedb.v1.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Uses {@link DriverSupport} to infer the {@link Driver} implementation unless the driver class name is given.
 */
public class DriverSupportDataSource implements ConnectionProvider {
    private Driver driver;
    private final String url;
    private final String user;
    private final String password;
    private final Properties properties;
    private final DriverSupport driverSupport;

    /**
     * Creates a new DriverDataSource.
     *
     * @param classLoader The ClassLoader to use.
     * @param driverClass The name of the JDBC Driver class to use. {@code null} for url-based autodetection.
     * @param url         The JDBC URL to use for connecting through the Driver. (required)
     * @param user        The JDBC user to use for connecting through the Driver.
     * @param password    The JDBC password to use for connecting through the Driver.
     * @param properties  Properties to pass to the JDBC driver when opening connections.
     * @throws MigrateDbException if the datasource could not be created.
     */
    public DriverSupportDataSource(ClassLoader classLoader,
                                   @Nullable String driverClass,
                                   String url,
                                   @Nullable String user,
                                   @Nullable String password,
                                   Map<String, String> properties,
                                   DriverSupport driverSupport) throws MigrateDbException {
        this.url = Objects.requireNonNull(url);
        this.driverSupport = Objects.requireNonNull(driverSupport);

        if (!StringUtils.hasLength(driverClass)) {
            driverClass = driverSupport.getDriverClass(url, classLoader);
        }

        this.properties = new Properties();
        properties.forEach(this.properties::setProperty);

        try {
            this.driver = ClassUtils.instantiate(driverClass, classLoader);
        } catch (MigrateDbException e) {
            String backupDriverClass = driverSupport.getBackupDriverClass(url, classLoader);
            if (backupDriverClass == null) {
                String extendedError = driverSupport.instantiateClassExtendedErrorMessage();
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

        this.user = user;
        this.password = password;

        if (driverSupport.externalAuthPropertiesRequired(url, user, password)) {
            properties.putAll(driverSupport.getExternalAuthProperties(url, user));
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnectionFromDriver(user, password);
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
    private Connection getConnectionFromDriver(@Nullable String username, @Nullable String password) throws SQLException {
        Properties properties = new Properties(this.properties);

        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }

        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new MigrateDbException("Unable to connect to " + driverSupport.redactJdbcUrl(url));
        }
        connection.setAutoCommit(true);
        return connection;
    }
}
