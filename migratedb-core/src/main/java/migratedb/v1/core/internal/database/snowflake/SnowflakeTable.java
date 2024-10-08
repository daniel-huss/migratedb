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

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseTable;

import java.sql.SQLException;
import java.util.List;

public class SnowflakeTable extends BaseTable {
    SnowflakeTable(JdbcTemplate jdbcTemplate, SnowflakeDatabase database, SnowflakeSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        if (!getSchema().exists()) {
            return false;
        }

        String sql = "SHOW TABLES LIKE '" + getName() + "' IN SCHEMA " + getDatabase().quote(getSchema().getName());
        List<Boolean> results = jdbcTemplate.query(sql, rs -> true);
        return !results.isEmpty();
    }

    @Override
    protected void doLock() throws SQLException {
        // no-op
    }
}
