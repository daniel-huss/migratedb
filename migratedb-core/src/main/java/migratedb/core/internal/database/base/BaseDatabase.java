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

import migratedb.core.api.MigrationType;
import migratedb.core.api.Version;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.*;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.internal.sqlscript.Delimiter;
import migratedb.core.api.internal.sqlscript.SqlScript;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.exception.MigrateDbUpgradeRequiredException;
import migratedb.core.internal.jdbc.JdbcUtils;
import migratedb.core.internal.resource.StringResource;
import migratedb.core.internal.util.AbbreviationUtils;
import migratedb.core.internal.util.StringUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Abstraction for database-specific functionality.
 */
public abstract class BaseDatabase<C extends Connection<?>> implements Database<C> {
    private static final Log LOG = Log.getLog(BaseDatabase.class);

    protected final DatabaseType databaseType;
    protected final Configuration configuration;
    protected final JdbcConnectionFactory jdbcConnectionFactory;
    protected final DatabaseMetaData jdbcMetaData;
    protected JdbcTemplate jdbcTemplate;
    private C migrationConnection;
    private C mainConnection;
    /**
     * The main JDBC connection, without any wrapping.
     */
    protected final java.sql.Connection rawMainJdbcConnection;
    /**
     * The 'major.minor' version of this database.
     */
    private Version version;
    /**
     * The user who applied the migrations.
     */
    private String installedBy;

    public BaseDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        this.databaseType = jdbcConnectionFactory.getDatabaseType();
        this.configuration = configuration;
        this.rawMainJdbcConnection = jdbcConnectionFactory.openConnection();
        try {
            this.jdbcMetaData = rawMainJdbcConnection.getMetaData();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to get metadata for connection", e);
        }
        this.jdbcTemplate = new JdbcTemplate(rawMainJdbcConnection, databaseType);
        this.jdbcConnectionFactory = jdbcConnectionFactory;
    }

    /**
     * Retrieves a MigrateDB Connection for this JDBC connection.
     */
    private C getConnection(java.sql.Connection connection) {
        return doGetConnection(connection);
    }

    /**
     * Retrieves a MigrateDB Connection for this JDBC connection.
     */
    protected abstract C doGetConnection(java.sql.Connection connection);

    @Override
    public final Version getVersion() {
        if (version == null) {
            version = determineVersion();
        }
        return version;
    }

    protected final void ensureDatabaseIsRecentEnough(String oldestSupportedVersion) {
        if (!getVersion().isAtLeast(oldestSupportedVersion)) {
            throw new MigrateDbUpgradeRequiredException(
                    databaseType,
                    computeVersionDisplayName(getVersion()),
                    computeVersionDisplayName(Version.parse(oldestSupportedVersion)));
        }
    }

    protected final void recommendMigrateDbUpgradeIfNecessary(String newestSupportedVersion) {
        if (getVersion().isNewerThan(newestSupportedVersion)) {
            recommendMigrateDbUpgrade(newestSupportedVersion);
        }
    }

    protected final void recommendMigrateDbUpgradeIfNecessaryForMajorVersion(String newestSupportedVersion) {
        if (getVersion().isMajorNewerThan(newestSupportedVersion)) {
            recommendMigrateDbUpgrade(newestSupportedVersion);
        }
    }

    private void recommendMigrateDbUpgrade(String newestSupportedVersion) {
        String message =
                "MigrateDB upgrade recommended: " + databaseType + " " + computeVersionDisplayName(getVersion())
                        + " is newer than this version of MigrateDB and support has not been tested."
                        + " The latest supported version of " + databaseType + " is " + newestSupportedVersion + ".";
        LOG.warn(message);
    }

    /**
     * Compute the user-friendly display name for this database version.
     */
    protected String computeVersionDisplayName(Version version) {
        return version.toString();
    }

    @Override
    public Delimiter getDefaultDelimiter() {
        return Delimiter.SEMICOLON;
    }

    @Override
    public final String getCatalog() {
        try {
            return doGetCatalog();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error retrieving the database name", e);
        }
    }

    protected String doGetCatalog() throws SQLException {
        return getMainConnection().getJdbcConnection().getCatalog();
    }

    @Override
    public final String getCurrentUser() {
        try {
            return doGetCurrentUser();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error retrieving the database user", e);
        }
    }

    protected String doGetCurrentUser() throws SQLException {
        return jdbcMetaData.getUserName();
    }

    @Override
    public final String quote(String... identifiers) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (String identifier : identifiers) {
            if (!first) {
                result.append(".");
            }
            first = false;
            result.append(doQuote(identifier));
        }

        return result.toString();
    }

    /**
     * Quotes this identifier for use in SQL queries.
     */
    protected String doQuote(String identifier) {
        return getOpenQuote() + identifier + getCloseQuote();
    }

    protected String getOpenQuote() {
        return "\"";
    }

    protected String getCloseQuote() {
        return "\"";
    }

    protected String getEscapedQuote() {
        return "";
    }

    @Override
    public String unQuote(String identifier) {
        String open = getOpenQuote();
        String close = getCloseQuote();

        if (!open.equals("") && !close.equals("") && identifier.startsWith(open) && identifier.endsWith(close)) {
            identifier = identifier.substring(open.length(), identifier.length() - close.length());
            if (!getEscapedQuote().equals("")) {
                identifier = StringUtils.replaceAll(identifier, getEscapedQuote(), close);
            }
        }

        return identifier;
    }

    @Override
    public boolean useSingleConnection() {
        return false;
    }

    @Override
    public DatabaseMetaData getJdbcMetaData() {
        return jdbcMetaData;
    }

    @Override
    public final C getMainConnection() {
        if (mainConnection == null) {
            this.mainConnection = getConnection(rawMainJdbcConnection);
        }
        return mainConnection;
    }

    @Override
    public final C getMigrationConnection() {
        if (migrationConnection == null) {
            if (useSingleConnection()) {
                this.migrationConnection = getMainConnection();
            } else {
                this.migrationConnection = getConnection(jdbcConnectionFactory.openConnection());
            }
        }
        return migrationConnection;
    }

    /**
     * @return The major and minor version of the database.
     */
    protected Version determineVersion() {
        try {
            return Version.parse(
                    jdbcMetaData.getDatabaseMajorVersion() + "." + jdbcMetaData.getDatabaseMinorVersion());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine the major version of the database", e);
        }
    }

    @Override
    public final SqlScript getCreateScript(SqlScriptFactory sqlScriptFactory, Table<?, ?> table, boolean baseline) {
        return sqlScriptFactory.createSqlScript(new StringResource("", getRawCreateScript(table, baseline)),
                false,
                null);
    }

    @Override
    public String getInsertStatement(Table<?, ?> table) {
        return "INSERT INTO " + table
                + " (" + quote("installed_rank")
                + ", " + quote("version")
                + ", " + quote("description")
                + ", " + quote("type")
                + ", " + quote("script")
                + ", " + quote("checksum")
                + ", " + quote("installed_by")
                + ", " + quote("execution_time")
                + ", " + quote("success")
                + ")"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    public final String getBaselineStatement(Table<?, ?> table) {
        return String.format(getInsertStatement(table).replace("?", "%s"),
                1,
                "'" + configuration.getBaselineVersion() + "'",
                "'" + AbbreviationUtils.abbreviateDescription(configuration.getBaselineDescription()) +
                        "'",
                "'" + MigrationType.BASELINE + "'",
                "'" + AbbreviationUtils.abbreviateScript(configuration.getBaselineDescription()) + "'",
                "NULL",
                "'" + installedBy + "'",
                0,
                getBooleanTrue()
        );
    }

    @Override
    public String getSelectStatement(Table<?, ?> table) {
        return "SELECT " + quote("installed_rank")
                + "," + quote("version")
                + "," + quote("description")
                + "," + quote("type")
                + "," + quote("script")
                + "," + quote("checksum")
                + "," + quote("installed_on")
                + "," + quote("installed_by")
                + "," + quote("execution_time")
                + "," + quote("success")
                + " FROM " + table
                + " WHERE " + quote("installed_rank") + " > ?"
                + " ORDER BY " + quote("installed_rank");
    }

    @Override
    public final String getInstalledBy() {
        if (installedBy == null) {
            installedBy = configuration.getInstalledBy() == null ? getCurrentUser() : configuration.getInstalledBy();
        }
        return installedBy;
    }

    @Override
    public void close() {
        if (mainConnection == null && migrationConnection == null) {
            if (rawMainJdbcConnection != null) JdbcUtils.closeConnection(rawMainJdbcConnection);
        } else if (mainConnection == migrationConnection) {
            mainConnection.close();
        } else {
            try {
                if (migrationConnection != null) migrationConnection.close();
            } finally {
                if (mainConnection != null) mainConnection.close();
            }
        }
    }

    @Override
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public boolean supportsEmptyMigrationDescription() {
        return true;
    }

    @Override
    public boolean supportsMultiStatementTransactions() {
        return true;
    }

    @Override
    public void cleanPreSchemas() {
        try {
            doCleanPreSchemas();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to clean database " + this, e);
        }
    }

    /**
     * Cleans all the objects in this database that need to be cleaned before each schema.
     *
     * @throws SQLException when the clean failed.
     */
    protected void doCleanPreSchemas() throws SQLException {
    }

    @Override
    public void cleanPostSchemas(Schema<?, ?>[] schemas) {
        try {
            doCleanPostSchemas(schemas);
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to clean schema " + this, e);
        }
    }

    /**
     * Cleans all the objects in this database that need to be cleaned after each schema.
     *
     * @param schemas The list of schemas managed by MigrateDB.
     * @throws SQLException when the clean failed.
     */
    protected void doCleanPostSchemas(Schema<?, ?>[] schemas) throws SQLException {
    }

    @Override
    public Schema<?, ?>[] getAllSchemas() {
        throw new UnsupportedOperationException("Getting all schemas not supported for " + getDatabaseType().getName());
    }
}
