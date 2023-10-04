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
package migratedb.v1.core.internal.database.informix;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Informix implementation of Schema.
 */
public class InformixSchema extends BaseSchema {
    /**
     * Creates a new Informix schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    InformixSchema(JdbcTemplate jdbcTemplate, InformixDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM systables where owner = ? and tabid > 99", name) > 0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return doAllTables().isEmpty();
    }

    @Override
    protected void doCreate() {
    }

    private List<InformixTable> findTables(String sqlQuery, String... params) throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(sqlQuery, params);
        List<InformixTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new InformixTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    protected InformixDatabase getDatabase() {
        return (InformixDatabase) super.getDatabase();
    }

    @Override
    protected List<InformixTable> doAllTables() throws SQLException {
        return findTables("SELECT t.tabname FROM \"informix\".systables AS t" +
                          " WHERE owner=? AND t.tabid > 99 AND t.tabtype='T'" +
                          " AND t.tabname NOT IN (" +
                          // Exclude Informix TimeSeries tables
                          " 'calendarpatterns', 'calendartable'," +
                          " 'tscontainertable', 'tscontainerwindowtable', 'tsinstancetable', " +
                          " 'tscontainerusageactivewindowvti', 'tscontainerusagedormantwindowvti'" +
                          ")", name);
    }

    @Override
    public InformixTable getTable(String tableName) {
        return new InformixTable(jdbcTemplate, getDatabase(), this, tableName);
    }
}
