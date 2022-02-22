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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.DatabaseType;
import migratedb.core.internal.exception.MigrateDbSqlException;

/**
 * Utility class for dealing with jdbc connections.
 */
public class JdbcConnectionFactory {
    private static final Log LOG = Log.getLog(JdbcConnectionFactory.class);

    private final DataSource dataSource;
    private final int connectRetries;
    private final int connectRetriesInterval;
    private final Configuration configuration;
    private final DatabaseType databaseType;
    private final String jdbcUrl;
    private final String driverInfo;
    private final String productName;

    private Connection firstConnection;
    private ConnectionInitializer connectionInitializer;

    /**
     * Creates a new JDBC connection factory. This automatically opens a first connection which can be obtained via a
     * call to getConnection and which must be closed again to avoid leaking it.
     *
     * @param dataSource           The DataSource to obtain the connection from.
     * @param configuration        The MigrateDb configuration.
     * @param statementInterceptor The statement interceptor. {@code null} if none.
     */
    public JdbcConnectionFactory(DataSource dataSource, Configuration configuration,
                                 StatementInterceptor statementInterceptor) {
        this.dataSource = dataSource;
        this.connectRetries = configuration.getConnectRetries();
        this.connectRetriesInterval = configuration.getConnectRetriesInterval();
        this.configuration = configuration;

        firstConnection = JdbcUtils.openConnection(dataSource,
                                                   connectRetries,
                                                   connectRetriesInterval,
                                                   configuration.getDatabaseTypeRegister());
        this.databaseType = configuration.getDatabaseTypeRegister().getDatabaseTypeForConnection(firstConnection);

        DatabaseMetaData databaseMetaData = JdbcUtils.getDatabaseMetaData(firstConnection);
        this.jdbcUrl = getJdbcUrl(databaseMetaData);
        this.driverInfo = getDriverInfo(databaseMetaData);
        this.productName = JdbcUtils.getDatabaseProductName(databaseMetaData);

        firstConnection = databaseType.alterConnectionAsNeeded(firstConnection, configuration);
    }

    public void setConnectionInitializer(ConnectionInitializer connectionInitializer) {
        this.connectionInitializer = connectionInitializer;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDriverInfo() {
        return driverInfo;
    }

    public String getProductName() {
        return productName;
    }

    public Connection openConnection() throws MigrateDbException {
        Connection connection = firstConnection == null ? JdbcUtils.openConnection(dataSource,
                                                                                   connectRetries,
                                                                                   connectRetriesInterval,
                                                                                   configuration.getDatabaseTypeRegister())
                                                        : firstConnection;
        firstConnection = null;

        if (connectionInitializer != null) {
            connectionInitializer.initialize(this, connection);
        }

        connection = databaseType.alterConnectionAsNeeded(connection, configuration);
        return connection;
    }

    public interface ConnectionInitializer {
        void initialize(JdbcConnectionFactory jdbcConnectionFactory, Connection connection);
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
    static String filterUrl(String url) {
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
}