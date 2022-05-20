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
package migratedb.core.internal.database.informix;

import java.sql.Connection;
import java.sql.SQLException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.database.base.BaseDatabase;

/**
 * Informix database.
 */
public class InformixDatabase extends BaseDatabase<InformixConnection> {
    /**
     * Creates a new instance.
     *
     * @param configuration The MigrateDb configuration.
     */
    public InformixDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                            StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected InformixConnection doGetConnection(Connection connection) {
        return new InformixConnection(this, connection);
    }

    @Override
    public final void ensureSupported() {
        ensureDatabaseIsRecentEnough("12.10");
        recommendMigrateDbUpgradeIfNecessary("12.10");
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        String tablespace = configuration.getTablespace() == null
                            ? ""
                            : " IN \"" + configuration.getTablespace() + "\"";

        return "CREATE TABLE " + table + " (\n" +
               "    installed_rank INT NOT NULL,\n" +
               "    version VARCHAR(50),\n" +
               "    description VARCHAR(200) NOT NULL,\n" +
               "    type VARCHAR(20) NOT NULL,\n" +
               "    script LVARCHAR(1000) NOT NULL,\n" +
               "    checksum VARCHAR(100),\n" +
               "    installed_by VARCHAR(100) NOT NULL,\n" +
               "    installed_on DATETIME YEAR TO FRACTION(3) DEFAULT CURRENT YEAR TO FRACTION(3) NOT NULL,\n" +
               "    execution_time INT NOT NULL,\n" +
               "    success SMALLINT NOT NULL\n" +
               ")" + tablespace + ";\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "ALTER TABLE " + table + " ADD CONSTRAINT CHECK (success in (0,1)) CONSTRAINT " + table.getName() +
               "_s;\n" +
               "ALTER TABLE " + table + " ADD CONSTRAINT PRIMARY KEY (installed_rank) CONSTRAINT " + table.getName() +
               "_pk;\n" +
               "CREATE INDEX " + table.getName() + "_s_idx ON " + table + " (success);";
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getJdbcMetaData().getUserName();
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

    @Override
    public String getOpenQuote() {
        return "";
    }

    @Override
    public String getCloseQuote() {
        return "";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean useSingleConnection() {
        return false;
    }
}
