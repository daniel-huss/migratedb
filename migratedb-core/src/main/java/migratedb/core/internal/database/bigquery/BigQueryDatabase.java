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
package migratedb.core.internal.database.bigquery;

import java.sql.Connection;
import java.sql.SQLException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.database.base.BaseDatabase;
import migratedb.core.internal.util.StringUtils;

public class BigQueryDatabase extends BaseDatabase<BigQueryConnection> {
    public BigQueryDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                            StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected BigQueryConnection doGetConnection(Connection connection) {
        return new BigQueryConnection(this, connection);
    }

    @Override
    public final void ensureSupported() {
        // BigQuery version is always at latest and supported
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE " + table + " (\n" +
               "    `installed_rank` INT64 NOT NULL,\n" +
               "    `version` STRING,\n" +
               "    `description` STRING NOT NULL,\n" +
               "    `type` STRING NOT NULL,\n" +
               "    `script` STRING NOT NULL,\n" +
               "    `checksum` INT64,\n" +
               "    `installed_by` STRING NOT NULL,\n" +
               "    `installed_on` TIMESTAMP,\n" + // BigQuery does not support default value
               "    `execution_time` INT64 NOT NULL,\n" +
               "    `success` BOOL NOT NULL\n" +
               ");\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "");
    }

    @Override
    public String getInsertStatement(Table table) {
        // Explicitly set installed_on to CURRENT_TIMESTAMP().
        return "INSERT INTO " + table
               + " (" + quote("installed_rank")
               + ", " + quote("version")
               + ", " + quote("description")
               + ", " + quote("type")
               + ", " + quote("script")
               + ", " + quote("checksum")
               + ", " + quote("installed_by")
               + ", " + quote("installed_on")
               + ", " + quote("execution_time")
               + ", " + quote("success")
               + ")"
               + " VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), ?, ?)";
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("SELECT SESSION_USER() as user;");
    }

    @Override
    public boolean supportsDdlTransactions() {
        // BigQuery is non-transactional
        return false;
    }

    @Override
    public boolean supportsChangingCurrentSchema() {
        // BigQuery has no concept of a current schema
        return false;
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
    public String getOpenQuote() {
        return "`";
    }

    @Override
    public String getCloseQuote() {
        return "`";
    }

    @Override
    public String getEscapedQuote() {
        return "\\`";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean supportsMultiStatementTransactions() {
        return false;
    }

    @Override
    public boolean useSingleConnection() {
        return true;
    }
}
