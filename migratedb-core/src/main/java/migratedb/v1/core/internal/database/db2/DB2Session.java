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
package migratedb.v1.core.internal.database.db2;

import migratedb.v1.core.internal.database.base.BaseSession;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DB2 connection.
 */
public class DB2Session extends BaseSession {
    DB2Session(DB2Database database, Connection connection) {
        super(database, connection);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return jdbcTemplate.queryForString("select current_schema from sysibm.sysdummy1");
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        jdbcTemplate.execute("SET SCHEMA " + getDatabase().quote(schema));
    }

    @Override
    public DB2Schema getSchema(String name) {
        return new DB2Schema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public DB2Database getDatabase() {
        return (DB2Database) super.getDatabase();
    }
}
