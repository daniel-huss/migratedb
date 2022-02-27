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
package migratedb.core.internal.database.spanner;

import java.sql.Connection;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.internal.database.base.BaseDatabase;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;

public class SpannerDatabase extends BaseDatabase<SpannerConnection> {
    public SpannerDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                           StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected SpannerConnection doGetConnection(Connection connection) {
        return new SpannerConnection(this, connection);
    }

    @Override
    public void ensureSupported() {
        recommendMigrateDbUpgradeIfNecessaryForMajorVersion("1.0");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    @Override
    public boolean supportsMultiStatementTransactions() {
        return false;
    }

    Connection getNewRawConnection() {
        return jdbcConnectionFactory.openConnection();
    }

    @Override
    public boolean supportsChangingCurrentSchema() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "true";
    }

    @Override
    public String getBooleanFalse() {
        return "false";
    }

    @Override
    protected String doQuote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean useSingleConnection() {
        return false;
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "" +
               "CREATE TABLE " + table.getName() + " (\n" +
               "    installed_rank INT64 NOT NULL,\n" +
               "    version STRING(50),\n" +
               "    description STRING(200) NOT NULL,\n" +
               "    type STRING(20) NOT NULL,\n" +
               "    script STRING(1000) NOT NULL,\n" +
               "    checksum INT64,\n" +
               "    installed_by STRING(100) NOT NULL,\n" +
               "    installed_on TIMESTAMP OPTIONS (allow_commit_timestamp=true),\n" +
               "    execution_time INT64 NOT NULL,\n" +
               "    success BOOL NOT NULL\n" +
               ") PRIMARY KEY (installed_rank DESC);\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "CREATE INDEX " + table.getName() + "_s_idx ON " + table.getName() + " (success);";
    }
}
