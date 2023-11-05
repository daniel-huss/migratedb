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
package migratedb.v1.core.internal.database.sqlserver;

import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.sqlscript.Delimiter;
import migratedb.v1.core.internal.database.base.BaseDatabase;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLServerDatabase extends BaseDatabase {
    public SQLServerDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        super(configuration, jdbcConnectionFactory);
    }

    @Override
    protected SQLServerSession doGetSession(Connection connection) {
        return new SQLServerSession(this, connection);
    }

    @Override
    public final void ensureSupported() {
        if (isAzure()) {
            ensureDatabaseIsRecentEnough("11.0");
            recommendMigrateDbUpgradeIfNecessary("16.0");
        } else {
            ensureDatabaseIsRecentEnough("10.0");
            recommendMigrateDbUpgradeIfNecessary("16.0");
        }
    }

    @Override
    protected String computeVersionDisplayName(Version version) {
        if (isAzure()) {
            return "Azure v" + getVersion().getMajorAsString();
        }
        if (getVersion().isAtLeast("8")) {
            if ("8".equals(getVersion().getMajorAsString())) {
                return "2000";
            }
            if ("9".equals(getVersion().getMajorAsString())) {
                return "2005";
            }
            if ("10".equals(getVersion().getMajorAsString())) {
                if ("0".equals(getVersion().getMinorAsString())) {
                    return "2008";
                }
                return "2008 R2";
            }
            if ("11".equals(getVersion().getMajorAsString())) {
                return "2012";
            }
            if ("12".equals(getVersion().getMajorAsString())) {
                return "2014";
            }
            if ("13".equals(getVersion().getMajorAsString())) {
                return "2016";
            }
            if ("14".equals(getVersion().getMajorAsString())) {
                return "2017";
            }
            if ("15".equals(getVersion().getMajorAsString())) {
                return "2019";
            }
            if ("16".equals(getVersion().getMajorAsString())) {
                return "2022";
            }
        }
        return super.computeVersionDisplayName(version);
    }

    @Override
    public Delimiter getDefaultDelimiter() {
        return Delimiter.GO;
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return this.getMainSession().getJdbcTemplate().queryForString("SELECT SUSER_SNAME()");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return true;
    }

    @Override
    public boolean supportsChangingCurrentSchema() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "1";
    }

    @Override
    public String getBooleanFalse() {
        return "0";
    }

    /**
     * Escapes this identifier, so it can be safely used in sql queries.
     *
     * @param identifier The identifier to escaped.
     * @return The escaped version.
     */
    private String escapeIdentifier(String identifier) {
        return StringUtils.replaceAll(identifier, getCloseQuote(), getEscapedQuote());
    }

    @Override
    public String doQuote(String identifier) {
        return getOpenQuote() + escapeIdentifier(identifier) + getCloseQuote();
    }

    @Override
    public String getOpenQuote() {
        return "[";
    }

    @Override
    public String getCloseQuote() {
        return "]";
    }

    @Override
    public String getEscapedQuote() {
        return "]]";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean usesSingleSingle() {
        return true;
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        String filegroup = isAzure() || configuration.getTablespace() == null ?
                "" : " ON \"" + configuration.getTablespace() + "\"";

        return "CREATE TABLE " + table + " (\n" +
               "    [installed_rank] INT NOT NULL,\n" +
               "    [" + "version] NVARCHAR(50),\n" +
               "    [description] NVARCHAR(200),\n" +
               "    [type] NVARCHAR(20) NOT NULL,\n" +
               "    [script] NVARCHAR(1000) NOT NULL,\n" +
               "    [checksum] NVARCHAR(100),\n" +
               "    [installed_by] NVARCHAR(100) NOT NULL,\n" +
               "    [installed_on] DATETIME NOT NULL DEFAULT GETDATE(),\n" +
               "    [execution_time] INT NOT NULL,\n" +
               "    [success] BIT NOT NULL\n" +
               ")" + filegroup + ";\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "ALTER TABLE " + table + " ADD CONSTRAINT [" + table.getName() +
               "_pk] PRIMARY KEY ([installed_rank]);\n" +
               "CREATE INDEX [" + table.getName() + "_s_idx] ON " + table + " ([success]);\n" +
               "GO\n";
    }

    private boolean isAzure() {
        return this.getMainSession().isAzureConnection();
    }

    private SQLServerEngineEdition getEngineEdition() {
        return this.getMainSession().getEngineEdition();
    }

    @Override
    public SQLServerSession getMainSession() {
        return (SQLServerSession) super.getMainSession();
    }

    protected boolean supportsPartitions() {
        return isAzure() || SQLServerEngineEdition.ENTERPRISE.equals(getEngineEdition()) ||
               getVersion().isAtLeast("13");
    }

    protected boolean supportsSequences() {
        return getVersion().isAtLeast("11");
    }

    protected boolean supportsSynonyms() {
        return true;
    }

    protected boolean supportsRules() {
        return true;
    }

    protected boolean supportsTypes() {
        return true;
    }

    protected boolean supportsTriggers() {
        return true;
    }

    protected boolean supportsAssemblies() {
        return true;
    }
}
