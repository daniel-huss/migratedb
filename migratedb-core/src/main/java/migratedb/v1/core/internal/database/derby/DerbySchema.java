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
package migratedb.v1.core.internal.database.derby;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Derby implementation of Schema.
 */
public class DerbySchema extends BaseSchema {
    /**
     * Creates a new Derby schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    public DerbySchema(JdbcTemplate jdbcTemplate, DerbyDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT (*) FROM sys.sysschemas WHERE schemaname=?", name) > 0;
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
    protected List<DerbyTable> doAllTables() throws SQLException {
        List<String> tableNames = listObjectNames("TABLE", "TABLETYPE='T'");

        List<DerbyTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new DerbyTable(jdbcTemplate, database(), this, tableName));
        }
        return tables;
    }

    /**
     * List the names of the objects of this type in this schema.
     *
     * @param objectType  The type of objects to list (Sequence, constant, ...)
     * @param querySuffix Suffix to append to the query to find the objects to list.
     * @return The names of the objects.
     * @throws SQLException when the object names could not be listed.
     */
    private List<String> listObjectNames(String objectType, String querySuffix) throws SQLException {
        String query = "SELECT " + objectType + "name FROM sys.sys" + objectType +
                       "s WHERE schemaid in (SELECT schemaid FROM sys.sysschemas where schemaname = ?)";
        if (StringUtils.hasLength(querySuffix)) {
            query += " AND " + querySuffix;
        }

        return jdbcTemplate.queryForStringList(query, name);
    }

    @Override
    public DerbyTable getTable(String tableName) {
        return new DerbyTable(jdbcTemplate, database(), this, tableName);
    }

    private DerbyDatabase database() {
        return (DerbyDatabase) super.getDatabase();
    }
}
