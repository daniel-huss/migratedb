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
package migratedb.v1.core.internal.database.saphana;

import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.internal.database.base.BaseSession;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.Connection;
import java.sql.SQLException;

public class SAPHANASession extends BaseSession {
    private final boolean isCloud;

    SAPHANASession(SAPHANADatabase database, Connection connection) {
        super(database, connection);
        try {
            String build = jdbcTemplate.queryForString("SELECT VALUE FROM M_HOST_INFORMATION WHERE KEY='build_branch'");
            // Cloud databases will be fa/CE<year>.<build> e.g. fa/CE2020.48
            // On-premise will be fa/hana<version>sp<servicepack> e.g. fa/hana2sp05
            isCloud = build.startsWith("fa/CE");
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to determine build edition", e);
        }
    }

    public boolean isCloudConnection() {
        return isCloud;
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return jdbcTemplate.queryForString("SELECT CURRENT_SCHEMA FROM DUMMY");
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        jdbcTemplate.execute("SET SCHEMA " + getDatabase().quote(schema));
    }

    @Override
    public Schema getSchema(String name) {
        return new SAPHANASchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public SAPHANADatabase getDatabase() {
        return (SAPHANADatabase) super.getDatabase();
    }
}
