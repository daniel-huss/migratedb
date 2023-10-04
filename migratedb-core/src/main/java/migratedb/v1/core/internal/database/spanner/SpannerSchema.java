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
package migratedb.v1.core.internal.database.spanner;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SpannerSchema extends BaseSchema {
    private static final Log LOG = Log.getLog(SpannerSchema.class);

    public SpannerSchema(JdbcTemplate jdbcTemplate, SpannerDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() {
        return name.isEmpty();
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        try (Connection c = getDatabase().getNewRawConnection()) {
            Statement s = c.createStatement();
            s.close();
            try (ResultSet tables = c.getMetaData().getTables("", "", null, null)) {
                return !tables.next();
            }
        }
    }

    @Override
    protected SpannerDatabase getDatabase() {
        return (SpannerDatabase) super.getDatabase();
    }

    @Override
    protected void doCreate() {
        LOG.info("Spanner does not support creating schemas. Schema not created: " + name);
    }

    @Override
    protected List<SpannerTable> doAllTables() throws SQLException {
        List<SpannerTable> tablesList = new ArrayList<>();
        Connection c = jdbcTemplate.getConnection();

        ResultSet tablesRs = c.getMetaData().getTables("", "", null, null);
        while (tablesRs.next()) {
            tablesList.add(new SpannerTable(jdbcTemplate, getDatabase(), this,
                                            tablesRs.getString("TABLE_NAME")));
        }
        tablesRs.close();

        return tablesList;
    }

    @Override
    public Table getTable(String tableName) {
        return new SpannerTable(jdbcTemplate, getDatabase(), this, tableName);
    }
}
