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
package migratedb.v1.core.internal.database.ignite.thin;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.internal.database.base.BaseDatabase;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Apache Ignite database.
 */
public class IgniteThinDatabase extends BaseDatabase {

    /**
     * Creates a new instance.
     *
     * @param configuration The MigrateDB configuration.
     */
    public IgniteThinDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        super(configuration, jdbcConnectionFactory);
    }

    @Override
    protected IgniteThinSession doGetSession(Connection connection) {
        return new IgniteThinSession(this, connection);
    }

    @Override
    protected Version determineVersion() {
        try {
            int buildId = getMainSession().getJdbcTemplate().queryForInt(
                    "SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'info.BUILD_ID'");
            return Version.parse(super.determineVersion() + "." + buildId);
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine Apache Ignite build ID", e);
        }
    }

    @Override
    public final void ensureSupported() {
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE IF NOT EXISTS " + table + " (\n" +
               "    \"installed_rank\" INT NOT NULL,\n" +
               "    \"version\" VARCHAR(50),\n" +
               "    \"description\" VARCHAR(200) NOT NULL,\n" +
               "    \"type\" VARCHAR(20) NOT NULL,\n" +
               "    \"script\" VARCHAR(1000) NOT NULL,\n" +
               "    \"checksum\" VARCHAR(100),\n" +
               "    \"installed_by\" VARCHAR(100) NOT NULL,\n" +
               "    \"installed_on\" TIMESTAMP NOT NULL,\n" +
               "    \"execution_time\" INT NOT NULL,\n" +
               "    \"success\" BOOLEAN NOT NULL,\n" +
               "     PRIMARY KEY (\"installed_rank\")\n" +
               ") WITH \"TEMPLATE=REPLICATED, BACKUPS=1,ATOMICITY=ATOMIC\";\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "CREATE INDEX IF NOT EXISTS \"" + table.getSchema().getName() + "\".\"" + table.getName() +
               "_s_idx\" ON " + table + " (\"success\");";
    }

    @Override
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
               // Ignore special table created marker
               + " WHERE " + quote("type") + " != 'TABLE'"
               + " AND " + quote("installed_rank") + " > ?"
               + " ORDER BY " + quote("installed_rank");
    }

    @Override
    public String getInsertStatement(Table table) {
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
               + " VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?,?)";
    }

    @Override
    protected String doGetCurrentUser() {
        String userName;
        try {
            Field connPropsField = getMainSession().getJdbcConnection().getClass().getDeclaredField("connProps");
            connPropsField.setAccessible(true);
            Object connProps = connPropsField.get(getMainSession().getJdbcConnection());
            userName = (String) connProps.getClass().getMethod("getUsername").invoke(connProps);
            if (userName == null || userName.isEmpty()) {
                return "ignite";
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new MigrateDbException(e);
        }
        return userName;
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
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
    public boolean catalogIsSchema() {
        return false;
    }
}
