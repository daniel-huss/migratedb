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
package migratedb.v1.core.internal.database.snowflake;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.internal.database.base.BaseSession;

import java.sql.Connection;
import java.sql.SQLException;

public class SnowflakeSession extends BaseSession {
    private final String originalRole;

    SnowflakeSession(SnowflakeDatabase database, Connection connection) {
        super(database, connection);
        try {
            this.originalRole = jdbcTemplate.queryForString("SELECT CURRENT_ROLE()");
        } catch (SQLException e) {
            throw new MigrateDbException("Unable to determine current role", e);
        }
    }

    @Override
    protected void doRestoreOriginalState() throws SQLException {
        // Reset the role to its original value in case a migration or callback changed it
        jdbcTemplate.execute("USE ROLE " + getDatabase().quote(originalRole));
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        String schemaName = jdbcTemplate.queryForString("SELECT CURRENT_SCHEMA()");
        return (schemaName != null) ? schemaName : "PUBLIC";
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        jdbcTemplate.execute("USE SCHEMA " + getDatabase().quote(schema));
    }

    @Override
    public SnowflakeSchema getSchema(String name) {
        return new SnowflakeSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public SnowflakeDatabase getDatabase() {
        return (SnowflakeDatabase) super.getDatabase();
    }
}
