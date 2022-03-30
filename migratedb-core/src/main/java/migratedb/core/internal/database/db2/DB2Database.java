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
package migratedb.core.internal.database.db2;

import java.sql.Connection;
import java.sql.SQLException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.database.base.BaseDatabase;

public class DB2Database extends BaseDatabase<DB2Connection> {
    public DB2Database(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                       StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected DB2Connection doGetConnection(Connection connection) {
        return new DB2Connection(this, connection);
    }

    @Override
    public final void ensureSupported() {
        ensureDatabaseIsRecentEnough("9.7");
        recommendMigrateDbUpgradeIfNecessary("11.5");
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        String tablespace = configuration.getTablespace() == null
                            ? ""
                            : " IN \"" + configuration.getTablespace() + "\"";

        return "CREATE TABLE " + table + " (\n" +
               "    \"installed_rank\" INT NOT NULL,\n" +
               "    \"version\" VARCHAR(50),\n" +
               "    \"description\" VARCHAR(200) NOT NULL,\n" +
               "    \"type\" VARCHAR(20) NOT NULL,\n" +
               "    \"script\" VARCHAR(1000) NOT NULL,\n" +
               "    \"checksum\" INT,\n" +
               "    \"installed_by\" VARCHAR(100) NOT NULL,\n" +
               "    \"installed_on\" TIMESTAMP DEFAULT CURRENT TIMESTAMP NOT NULL,\n" +
               "    \"execution_time\" INT NOT NULL,\n" +
               "    \"success\" SMALLINT NOT NULL,\n" +
               "    CONSTRAINT \"" + table.getName() + "_s\" CHECK (\"success\" in(0,1))\n" +
               ")" +

               " ORGANIZE BY ROW"

               + tablespace + ";\n" +
               "ALTER TABLE " + table + " ADD CONSTRAINT \"" + table.getName() +
               "_pk\" PRIMARY KEY (\"installed_rank\");\n" +
               "CREATE INDEX \"" + table.getSchema().getName() + "\".\"" + table.getName() + "_s_idx\" ON " + table +
               " (\"success\");" +
               (baseline ? getBaselineStatement(table) + ";\n" : "");
    }

    @Override
    public String getSelectStatement(Table table) {
        return super.getSelectStatement(table)
               // Allow uncommitted reads so info can be invoked while migrate is running
               + " WITH UR";
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("select CURRENT_USER from sysibm.sysdummy1");
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
        return "1";
    }

    @Override
    public String getBooleanFalse() {
        return "0";
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
