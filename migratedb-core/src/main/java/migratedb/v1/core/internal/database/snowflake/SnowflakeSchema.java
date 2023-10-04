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
package migratedb.v1.core.internal.database.snowflake;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.List;

public class SnowflakeSchema extends BaseSchema {
    /**
     * Creates a new Snowflake schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    SnowflakeSchema(JdbcTemplate jdbcTemplate, SnowflakeDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        List<Boolean> results = jdbcTemplate.query("SHOW SCHEMAS LIKE '" + name + "'", rs -> true);
        return !results.isEmpty();
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        int objectCount = getObjectCount("TABLE") + getObjectCount("VIEW")
                          + getObjectCount("SEQUENCE");

        return objectCount == 0;
    }

    private int getObjectCount(String objectType) throws SQLException {
        return jdbcTemplate.query("SHOW " + objectType + "S IN SCHEMA " + getDatabase().quote(name), rs -> 1).size();
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    @Override
    protected List<SnowflakeTable> doAllTables() throws SQLException {
        return jdbcTemplate.query("SHOW TABLES IN SCHEMA " + getDatabase().quote(name),
                                  rs -> {
                                      String tableName = rs.getString("name");
                                      return getTable(tableName);
                                  });
    }

    @Override
    public SnowflakeTable getTable(String tableName) {
        return new SnowflakeTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected SnowflakeDatabase getDatabase() {
        return (SnowflakeDatabase) super.getDatabase();
    }
}
