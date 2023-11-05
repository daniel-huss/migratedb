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
package migratedb.v1.core.internal.database.redshift;

import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.internal.database.base.BaseDatabase;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Redshift database.
 */
public class RedshiftDatabase extends BaseDatabase {
    /**
     * Creates a new instance.
     *
     * @param configuration The MigrateDB configuration.
     */
    public RedshiftDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        super(configuration, jdbcConnectionFactory);
    }

    @Override
    protected RedshiftSession doGetSession(Connection connection) {
        return new RedshiftSession(this, connection);
    }

    @Override
    public final void ensureSupported() {
        // Always latest Redshift version.
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE " + table + " (\n" +
               "    \"installed_rank\" INT NOT NULL SORTKEY,\n" +
               "    \"version\" VARCHAR(50),\n" +
               "    \"description\" VARCHAR(200) NOT NULL,\n" +
               "    \"type\" VARCHAR(20) NOT NULL,\n" +
               "    \"script\" VARCHAR(1000) NOT NULL,\n" +
               "    \"checksum\" VARCHAR(100),\n" +
               "    \"installed_by\" VARCHAR(100) NOT NULL,\n" +
               "    \"installed_on\" TIMESTAMP NOT NULL DEFAULT getdate(),\n" +
               "    \"execution_time\" INTEGER NOT NULL,\n" +
               "    \"success\" BOOLEAN NOT NULL\n" +
               ");\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "ALTER TABLE " + table + " ADD CONSTRAINT \"" + table.getName() +
               "_pk\" PRIMARY KEY (\"installed_rank\");";
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainSession().getJdbcTemplate().queryForString("SELECT current_user");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return true;
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

    @Override
    public boolean usesSingleSingle() {
        return false;
    }
}
