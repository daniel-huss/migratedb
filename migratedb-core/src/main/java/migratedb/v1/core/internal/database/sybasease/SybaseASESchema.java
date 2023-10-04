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
package migratedb.v1.core.internal.database.sybasease;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sybase ASE schema (database).
 */
public class SybaseASESchema extends BaseSchema {
    SybaseASESchema(JdbcTemplate jdbcTemplate, SybaseASEDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        //There is no schema in SAP ASE. Always return true
        return true;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        //There is no schema in SAP ASE, check whether database is empty
        //Check for tables, views stored procs and triggers
        return jdbcTemplate.queryForInt(
                "select count(*) from sysobjects ob where (ob.type='U' or ob.type = 'V' or ob.type = 'P' or ob.type = " +
                "'TR') and ob.name != 'sysquerymetrics'") ==
               0;
    }

    @Override
    protected void doCreate() {
        //There is no schema in SAP ASE. Do nothing for creation.
    }

    @Override
    protected List<SybaseASETable> doAllTables() throws SQLException {
        List<String> tableNames = retrieveAllTableNames();
        List<SybaseASETable> tables = new ArrayList<>(tableNames.size());
        for (String tableName : tableNames) {
            tables.add(new SybaseASETable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    protected SybaseASEDatabase getDatabase() {
        return (SybaseASEDatabase) super.getDatabase();
    }

    @Override
    public SybaseASETable getTable(String tableName) {
        return new SybaseASETable(jdbcTemplate, getDatabase(), this, tableName);
    }

    /**
     * @return all table names in the current database.
     */
    private List<String> retrieveAllTableNames() throws SQLException {
        return jdbcTemplate.queryForStringList("select ob.name from sysobjects ob where ob.type=? order by ob.name",
                                               "U");
    }
}
