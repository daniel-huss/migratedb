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
package migratedb.v1.core.internal.database.firebird;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FirebirdSchema extends BaseSchema {
    /**
     * Creates a new Firebird schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    public FirebirdSchema(JdbcTemplate jdbcTemplate, FirebirdDatabase database, String name) {
        super(jdbcTemplate, database, name);

    }

    @Override
    protected boolean doExists() throws SQLException {
        // database == schema, always return true
        return true;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        // database == schema, check content of database
        // Check for all object types except custom collations and roles
        return 0 == jdbcTemplate.queryForInt("select count(*)\n" +
                                             "from (\n" +
                                             "  -- views and tables\n" +
                                             "  select RDB$RELATION_NAME AS OBJECT_NAME\n" +
                                             "  from RDB$RELATIONS\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- stored procedures\n" +
                                             "  select RDB$PROCEDURE_NAME\n" +
                                             "  from RDB$PROCEDURES\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- triggers\n" +
                                             "  select RDB$TRIGGER_NAME\n" +
                                             "  from RDB$TRIGGERS\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- functions\n" +
                                             "  select RDB$FUNCTION_NAME\n" +
                                             "  from RDB$FUNCTIONS\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- sequences\n" +
                                             "  select RDB$GENERATOR_NAME\n" +
                                             "  from RDB$GENERATORS\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- exceptions\n" +
                                             "  select RDB$EXCEPTION_NAME\n" +
                                             "  from RDB$EXCEPTIONS\n" +
                                             "  where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "  union all\n" +
                                             "  -- domains\n" +
                                             "  select RDB$FIELD_NAME\n" +
                                             "  from RDB$FIELDS\n" +
                                             "  where RDB$FIELD_NAME not starting with 'RDB$'\n" +
                                             "  and (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)\n" +
                                             "union all\n" +
                                             "-- packages\n" +
                                             "select RDB$PACKAGE_NAME\n" +
                                             "from RDB$PACKAGES\n" +
                                             "where (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)) a");
    }

    @Override
    protected void doCreate() throws SQLException {
        // database == schema, do nothing for creation
    }

    @Override
    protected List<FirebirdTable> doAllTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(
                "select RDB$RELATION_NAME as tableName\n" +
                "from RDB$RELATIONS\n" +
                "where RDB$VIEW_BLR is null\n" +
                "and (RDB$SYSTEM_FLAG is null or RDB$SYSTEM_FLAG = 0)");

        List<FirebirdTable> tables = new ArrayList<>(tableNames.size());
        for (String tableName : tableNames) {
            tables.add(getTable(tableName));
        }

        return tables;
    }

    @Override
    public FirebirdTable getTable(String tableName) {
        return new FirebirdTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected FirebirdDatabase getDatabase() {
        return (FirebirdDatabase) super.getDatabase();
    }
}
