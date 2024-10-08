/*
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
package migratedb.v1.core.internal.jdbc;

import migratedb.v1.core.api.ConnectionProvider;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.DatabaseType;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Utility class for dealing with jdbc connections.
 */
public class JdbcConnectionFactoryImpl implements JdbcConnectionFactory, AutoCloseable {
    private final ConnectionProvider dataSource;
    private final int connectRetries;
    private final int connectRetriesInterval;
    private final Configuration configuration;
    private final DatabaseType databaseType;
    private final String jdbcUrl;
    private final String driverInfo;
    private final String productName;

    private Connection firstConnection;
    private ConnectionInitializer connectionInitializer;

    public JdbcConnectionFactoryImpl(ConnectionProvider dataSource, Configuration configuration) {
        this.dataSource = dataSource;
        this.connectRetries = configuration.getConnectRetries();
        this.connectRetriesInterval = configuration.getConnectRetriesInterval();
        this.configuration = configuration;

        this.firstConnection = JdbcUtils.openConnection(dataSource,
                                                        connectRetries,
                                                        connectRetriesInterval,
                                                        configuration.getDatabaseTypeRegister());
        try {
            this.databaseType = configuration.getDatabaseTypeRegister().getDatabaseTypeForConnection(firstConnection);

            DatabaseMetaData databaseMetaData = JdbcUtils.getDatabaseMetaData(firstConnection);
            this.jdbcUrl = getJdbcUrl(databaseMetaData);
            this.driverInfo = getDriverInfo(databaseMetaData);
            this.productName = JdbcUtils.getDatabaseProductName(databaseMetaData);
        } catch (RuntimeException | Error e) {
            JdbcUtils.closeConnection(firstConnection);
            throw e;
        }
    }

    public void setConnectionInitializer(ConnectionInitializer connectionInitializer) {
        this.connectionInitializer = connectionInitializer;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String getDriverInfo() {
        return driverInfo;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public Connection openConnection() throws MigrateDbException {
        var connection = firstConnection == null ? JdbcUtils.openConnection(dataSource,
                                                                            connectRetries,
                                                                            connectRetriesInterval,
                                                                            configuration.getDatabaseTypeRegister())
                : firstConnection;
        firstConnection = null;
        try {
            databaseType.alterConnectionAsNeeded(connection, configuration);
            if (connectionInitializer != null) {
                connectionInitializer.initialize(this, connection);
            }
        } catch (RuntimeException | Error e) {
            try {
                connection.close();
            } catch (RuntimeException | SQLException | Error suppressed) {
                if (!e.equals(suppressed)) {
                    e.addSuppressed(suppressed);
                }
            }
            throw e;
        }
        return connection;
    }

    private static String getJdbcUrl(DatabaseMetaData databaseMetaData) {
        String url;
        try {
            url = databaseMetaData.getURL();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to retrieve the JDBC connection URL!", e);
        }
        if (url == null) {
            return "";
        }
        return filterUrl(url);
    }

    /**
     * Filter out URL parameters to avoid including passwords etc.
     */
    private static String filterUrl(String url) {
        int questionMark = url.indexOf("?");
        if (questionMark >= 0 && !url.contains("?databaseName=")) {
            url = url.substring(0, questionMark);
        }
        url = url.replaceAll("://.*:.*@", "://");
        return url;
    }

    private static String getDriverInfo(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.getDriverName() + " " + databaseMetaData.getDriverVersion();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to read database driver info: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        JdbcUtils.closeConnection(firstConnection);
    }
}
