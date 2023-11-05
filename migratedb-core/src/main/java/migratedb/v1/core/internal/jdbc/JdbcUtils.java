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
package migratedb.v1.core.internal.jdbc;

import migratedb.v1.core.api.ConnectionProvider;
import migratedb.v1.core.api.DatabaseTypeRegister;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.strategy.BackoffStrategy;
import migratedb.v1.core.internal.util.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.*;

/**
 * Utility class for dealing with jdbc connections.
 */
public final class JdbcUtils {
    private static final Log LOG = Log.getLog(JdbcUtils.class);

    /**
     * Opens a new connection from {@code dataSource}.
     *
     * @param dataSource             The data source to obtain the connection from.
     * @param connectRetries         The maximum number of retries when attempting to connect to the database.
     * @param connectRetriesInterval The maximum time between retries in seconds
     * @return The new connection.
     * @throws MigrateDbException if the connection could not be opened.
     */
    @SuppressWarnings("BusyWait")
    public static Connection openConnection(ConnectionProvider dataSource,
                                            int connectRetries,
                                            int connectRetriesInterval,
                                            DatabaseTypeRegister databaseTypeRegister)
            throws MigrateDbException {
        BackoffStrategy backoffStrategy = new BackoffStrategy(1, 2, connectRetriesInterval);
        int retries = 0;
        while (true) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                if ("08S01".equals(e.getSQLState()) && e.getMessage().contains(
                        "This driver is not configured for integrated authentication")) {
                    throw new MigrateDbSqlException("Unable to obtain connection from database", e);
                } else if (e.getSQLState() == null && e.getMessage().contains("MSAL4J")) {
                    throw new MigrateDbSqlException("Unable to obtain connection from database.\n" +
                                                    "You need to install some extra drivers in order for interactive authentication to work.",
                                                    e);
                }

                if (++retries > connectRetries) {
                    throw new MigrateDbSqlException("Unable to obtain connection from database", e);
                }
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                String msg = "Connection error: " + e.getMessage();
                if (rootCause != null && rootCause != e && rootCause.getMessage() != null) {
                    msg += " (Caused by " + rootCause.getMessage() + ")";
                }
                LOG.warn(msg + " Retrying in " + backoffStrategy.peek() + " sec...");
                try {
                    Thread.sleep(backoffStrategy.next() * 1000L);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new MigrateDbSqlException("Unable to obtain connection from database", e);
                }
            }
        }
    }

    /**
     * Quietly closes this connection. This method never throws an exception even if closing the connection fails.
     */
    public static void closeConnection(@Nullable Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException | RuntimeException e) {
            LOG.error("Error while closing database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Quietly closes this statement. This method never throws an exception even if closing the statement fails.
     */
    public static void closeStatement(Statement statement) {
        if (statement == null) {
            return;
        }

        try {
            statement.close();
        } catch (SQLException | RuntimeException e) {
            LOG.error("Error while closing JDBC statement", e);
        }
    }

    /**
     * Quietly closes this resultSet. This method never throws an exception even if closing the result set fails.
     */
    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }

        try {
            resultSet.close();
        } catch (SQLException | RuntimeException e) {
            LOG.error("Error while closing JDBC resultSet", e);
        }
    }

    public static DatabaseMetaData getDatabaseMetaData(Connection connection) {
        DatabaseMetaData databaseMetaData;
        try {
            databaseMetaData = connection.getMetaData();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to read database connection metadata: " + e.getMessage(), e);
        }
        if (databaseMetaData == null) {
            throw new MigrateDbException("Unable to read database connection metadata while it is null!");
        }
        return databaseMetaData;
    }

    /**
     * @return The name of the database product. Example: Oracle, MySQL, ...
     */
    public static String getDatabaseProductName(DatabaseMetaData databaseMetaData) {
        try {
            String databaseProductName = databaseMetaData.getDatabaseProductName();
            if (databaseProductName == null) {
                throw new MigrateDbException("Unable to determine database. Product name is null.");
            }

            int databaseMajorVersion = databaseMetaData.getDatabaseMajorVersion();
            int databaseMinorVersion = databaseMetaData.getDatabaseMinorVersion();

            return databaseProductName + " " + databaseMajorVersion + "." + databaseMinorVersion;
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error while determining database product name", e);
        }
    }

    /**
     * @return The version of the database product. Example: MariaDB 10.3, ...
     */
    public static String getDatabaseProductVersion(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error while determining database product version", e);
        }
    }
}
