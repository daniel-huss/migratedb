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
package migratedb.v1.core.internal.database.saphana;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SAP HANA implementation of Schema.
 */
public class SAPHANASchema extends BaseSchema {
    /**
     * Creates a new SAP HANA schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    SAPHANASchema(JdbcTemplate jdbcTemplate, SAPHANADatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM SYS.SCHEMAS WHERE SCHEMA_NAME=?", name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        int objectCount = jdbcTemplate.queryForInt("select count(*) from sys.tables where schema_name = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from sys.views where schema_name = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from sys.sequences where schema_name = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from sys.synonyms where schema_name = ?", name);
        return objectCount == 0;
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    private List<String> getDbObjects(String objectType) throws SQLException {
        return jdbcTemplate.queryForStringList(
                "select " + objectType + "_NAME from SYS." + objectType + "S where SCHEMA_NAME = ?", name);
    }

    @Override
    protected List<SAPHANATable> doAllTables() throws SQLException {
        List<String> tableNames = getDbObjects("TABLE");
        List<SAPHANATable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new SAPHANATable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new SAPHANATable(jdbcTemplate, getDatabase(), this, tableName);
    }

    @Override
    protected SAPHANADatabase getDatabase() {
        return (SAPHANADatabase) super.getDatabase();
    }
}
