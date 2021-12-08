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

import java.io.Closeable;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import migratedb.core.api.MigrationType;
import migratedb.core.api.MigrationVersion;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.DatabaseType;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.exception.MigrateDbUpgradeRequiredException;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.JdbcTemplate;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.resource.StringResource;
import migratedb.core.internal.sqlscript.Delimiter;
import migratedb.core.internal.sqlscript.SqlScript;
import migratedb.core.internal.sqlscript.SqlScriptFactory;
import migratedb.core.internal.util.AbbreviationUtils;

/**
 * Abstraction for database-specific functionality.
 */
public abstract class Database<C extends Connection> implements Closeable {
    private static final Log LOG = Log.getLog(Database.class);

    protected final DatabaseType databaseType;
    protected final Configuration configuration;
    protected final StatementInterceptor statementInterceptor;
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
    private MigrationVersion version;
    /**
     * The user who applied the migrations.
     */
    private String installedBy;

    public Database(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                    StatementInterceptor statementInterceptor) {
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
        this.statementInterceptor = statementInterceptor;
    }

    /**
     * Retrieves a MigrateDb Connection for this JDBC connection.
     */
    private C getConnection(java.sql.Connection connection) {
        return doGetConnection(connection);
    }

    /**
     * Retrieves a MigrateDb Connection for this JDBC connection.
     */
    protected abstract C doGetConnection(java.sql.Connection connection);

    /**
     * Ensure MigrateDb supports this version of this database.
     */
    public abstract void ensureSupported();

    /**
     * @return The 'major.minor' version of this database.
     */
    public final MigrationVersion getVersion() {
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
                computeVersionDisplayName(MigrationVersion.fromVersion(oldestSupportedVersion)));
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
            "MigrateDb upgrade recommended: " + databaseType + " " + computeVersionDisplayName(getVersion())
            + " is newer than this version of MigrateDb and support has not been tested."
            + " The latest supported version of " + databaseType + " is " + newestSupportedVersion + ".";
        LOG.warn(message);
    }

    /**
     * Compute the user-friendly display name for this database version.
     */
    protected String computeVersionDisplayName(MigrationVersion version) {
        return version.getVersion();
    }

    public Delimiter getDefaultDelimiter() {
        return Delimiter.SEMICOLON;
    }

    /**
     * @return The name of the database, by default as determined by JDBC.
     */
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

    public abstract boolean supportsDdlTransactions();

    public abstract boolean supportsChangingCurrentSchema();

    /**
     * @return The representation of the value {@code true} in a boolean column.
     */
    public abstract String getBooleanTrue();

    /**
     * @return The representation of the value {@code false} in a boolean column.
     */
    public abstract String getBooleanFalse();

    /**
     * Quotes these identifiers for use in SQL queries. Multiple identifiers will be quoted and separated by a dot.
     */
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
    protected abstract String doQuote(String identifier);

    /**
     * @return {@code true} if this database uses a catalog to represent a schema, or {@code false} if a schema is
     * simply a schema.
     */
    public abstract boolean catalogIsSchema();

    /**
     * @return Whether to use a single connection for both schema history table management and applying migrations.
     */
    public boolean useSingleConnection() {
        return false;
    }

    public DatabaseMetaData getJdbcMetaData() {
        return jdbcMetaData;
    }

    /**
     * @return The main connection used to manipulate the schema history.
     */
    public final C getMainConnection() {
        if (mainConnection == null) {
            this.mainConnection = getConnection(rawMainJdbcConnection);
        }
        return mainConnection;
    }

    /**
     * @return The migration connection used to apply migrations.
     */
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
    protected MigrationVersion determineVersion() {
        try {
            return MigrationVersion.fromVersion(
                jdbcMetaData.getDatabaseMajorVersion() + "." + jdbcMetaData.getDatabaseMinorVersion());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine the major version of the database", e);
        }
    }

    /**
     * Retrieves the script used to create the schema history table.
     *
     * @param sqlScriptFactory The factory used to create the SQL script.
     * @param table            The table to create.
     * @param baseline         Whether to include the creation of a baseline marker.
     */
    public final SqlScript getCreateScript(SqlScriptFactory sqlScriptFactory, Table table, boolean baseline) {
        return sqlScriptFactory.createSqlScript(new StringResource(getRawCreateScript(table, baseline)), false, null);
    }

    public abstract String getRawCreateScript(Table table, boolean baseline);

    public String getInsertStatement(Table table) {
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

    public final String getBaselineStatement(Table table) {
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

    public String getSelectStatement(Table table) {
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

    public final String getInstalledBy() {
        if (installedBy == null) {
            installedBy = configuration.getInstalledBy() == null ? getCurrentUser() : configuration.getInstalledBy();
        }
        return installedBy;
    }

    public void close() {
        if (!useSingleConnection() && migrationConnection != null) {
            migrationConnection.close();
        }
        if (mainConnection != null) {
            mainConnection.close();
        }
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public boolean supportsEmptyMigrationDescription() {
        return true;
    }

    public boolean supportsMultiStatementTransactions() {
        return true;
    }

    /**
     * Cleans all the objects in this database that need to be cleaned before each schema.
     */
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

    /**
     * Cleans all the objects in this database that need to be cleaned after each schema.
     *
     * @param schemas The list of schemas managed by MigrateDb.
     */
    public void cleanPostSchemas(Schema[] schemas) {
        try {
            doCleanPostSchemas(schemas);
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to clean schema " + this, e);
        }
    }

    /**
     * Cleans all the objects in this database that need to be cleaned after each schema.
     *
     * @param schemas The list of schemas managed by MigrateDb.
     *
     * @throws SQLException when the clean failed.
     */
    protected void doCleanPostSchemas(Schema[] schemas) throws SQLException {
    }
}
