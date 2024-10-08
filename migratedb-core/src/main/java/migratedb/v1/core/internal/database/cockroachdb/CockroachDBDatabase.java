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
package migratedb.v1.core.internal.database.cockroachdb;

import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseDatabase;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

public class CockroachDBDatabase extends BaseDatabase {

    private final Version determinedVersion;

    public CockroachDBDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        super(configuration, jdbcConnectionFactory);
        this.determinedVersion = rawDetermineVersion();
    }

    @Override
    protected CockroachDBSession doGetSession(Connection connection) {
        return new CockroachDBSession(this, connection);
    }

    @Override
    public final void ensureSupported() {
        ensureDatabaseIsRecentEnough("1.1");
        recommendMigrateDbUpgradeIfNecessary("24.99");
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE IF NOT EXISTS " + table + " (\n" +
               "    \"installed_rank\" INT NOT NULL PRIMARY KEY,\n" +
               "    \"version\" VARCHAR(50),\n" +
               "    \"description\" VARCHAR(200) NOT NULL,\n" +
               "    \"type\" VARCHAR(20) NOT NULL,\n" +
               "    \"script\" VARCHAR(1000) NOT NULL,\n" +
               "    \"checksum\" VARCHAR(100),\n" +
               "    \"installed_by\" VARCHAR(100) NOT NULL,\n" +
               "    \"installed_on\" TIMESTAMP NOT NULL DEFAULT now(),\n" +
               "    \"execution_time\" INTEGER NOT NULL,\n" +
               "    \"success\" BOOLEAN NOT NULL\n" +
               ");\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "CREATE INDEX IF NOT EXISTS \"" + table.getName() + "_s_idx\" ON " + table + " (\"success\");";
    }

    private Version rawDetermineVersion() {
        String version;
        try {
            // Use rawMainJdbcConnection to avoid infinite recursion.
            JdbcTemplate template = new JdbcTemplate(rawMainJdbcConnection, databaseType);
            version = template.queryForString("SELECT value FROM crdb_internal.node_build_info where field='Version'");
            if (version == null) {
                version = template.queryForString("SELECT value FROM crdb_internal.node_build_info where field='Tag'");
            }
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine CockroachDB version", e);
        }
        int firstDot = version.indexOf(".");
        int majorVersion = Integer.parseInt(version.substring(1, firstDot));
        String minorPatch = version.substring(firstDot + 1);
        int minorVersion = Integer.parseInt(minorPatch.substring(0, minorPatch.indexOf(".")));
        return Version.parse(majorVersion + "." + minorVersion);
    }

    @Override
    protected Version determineVersion() {
        return determinedVersion;
    }

    boolean supportsSchemas() {
        return getVersion().isAtLeast("20.2");
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainSession().getJdbcTemplate().queryForString("SELECT * FROM [SHOW SESSION_USER]");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    @Override
    public boolean supportsChangingCurrentSchema() {
        return true;
    }

    @Override
    public String getBooleanTrue() {
        return "TRUE";
    }

    @Override
    public String getBooleanFalse() {
        return "FALSE";
    }

    @Override
    public String doQuote(String identifier) {
        return getOpenQuote() + StringUtils.replaceAll(identifier, getCloseQuote(), getEscapedQuote()) +
               getCloseQuote();
    }

    @Override
    public String getEscapedQuote() {
        return "\"\"";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }
}
