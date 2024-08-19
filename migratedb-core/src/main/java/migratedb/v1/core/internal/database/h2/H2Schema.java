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
package migratedb.v1.core.internal.database.h2;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class H2Schema extends BaseSchema {
    private final boolean requiresV2Metadata;

    H2Schema(JdbcTemplate jdbcTemplate, H2Database database, String name, boolean requiresV2Metadata) {
        super(jdbcTemplate, database, name);
        this.requiresV2Metadata = requiresV2Metadata;
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME=?", name) >
               0;
    }

    @Override
    protected boolean doCheckIfEmpty() {
        return allTables().isEmpty();
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    @Override
    protected List<H2Table> doAllTables() throws SQLException {
        List<String> tableNames = listObjectNames("TABLE",
                                                  "TABLE_TYPE = " + (requiresV2Metadata ? "'BASE TABLE'" : "'TABLE'"));

        List<H2Table> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new H2Table(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    protected H2Database getDatabase() {
        return (H2Database) super.getDatabase();
    }

    /**
     * List the names of the objects of this type in this schema.
     *
     * @param objectType  The type of objects to list (Sequence, constant, ...)
     * @param querySuffix Suffix to append to the query to find the objects to list.
     * @return The names of the objects.
     * @throws java.sql.SQLException when the object names could not be listed.
     */
    private List<String> listObjectNames(String objectType, String querySuffix) throws SQLException {
        String query = "SELECT " + objectType + "_NAME FROM INFORMATION_SCHEMA." + objectType
                       + "S WHERE " + objectType + "_SCHEMA = ?";
        if (StringUtils.hasLength(querySuffix)) {
            query += " AND " + querySuffix;
        }

        return jdbcTemplate.queryForStringList(query, name);
    }

    @Override
    public Table getTable(String tableName) {
        return new H2Table(jdbcTemplate, getDatabase(), this, tableName);
    }
}
