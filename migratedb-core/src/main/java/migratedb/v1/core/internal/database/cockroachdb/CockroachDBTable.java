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
package migratedb.v1.core.internal.database.cockroachdb;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseTable;

import java.sql.SQLException;

/**
 * CockroachDB-specific table.
 * <p>
 * Note that CockroachDB doesn't support table locks.
 */
public class CockroachDBTable extends BaseTable {
    private static final Log LOG = Log.getLog(CockroachDBTable.class);

    CockroachDBTable(JdbcTemplate jdbcTemplate, CockroachDBDatabase database, CockroachDBSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return new CockroachDBRetryingStrategy().execute(this::doExistsOnce);
    }

    protected boolean doExistsOnce() throws SQLException {
        if (getSchema().cockroachDB1) {
            return jdbcTemplate.queryForBoolean("SELECT EXISTS (\n" +
                                                "   SELECT 1\n" +
                                                "   FROM   information_schema.tables \n" +
                                                "   WHERE  table_schema = ?\n" +
                                                "   AND    table_name = ?\n" +
                                                ")", getSchema().getName(), getName());
        } else if (!getSchema().hasSchemaSupport) {
            return jdbcTemplate.queryForBoolean("SELECT EXISTS (\n" +
                                                "   SELECT 1\n" +
                                                "   FROM   information_schema.tables \n" +
                                                "   WHERE  table_catalog = ?\n" +
                                                "   AND    table_schema = 'public'\n" +
                                                "   AND    table_name = ?\n" +
                                                ")", getSchema().getName(), getName());
        } else {
            // There is a bug in CockroachDB v20.2.0-beta.* which causes the string equality operator to not work as
            // expected, therefore we apply a workaround using the like operator.
            // https://github.com/cockroachdb/cockroach/issues/55437
            String sql = "SELECT EXISTS (\n" +
                         "   SELECT 1\n" +
                         "   FROM   information_schema.tables \n" +
                         "   WHERE  table_schema = ?\n" +
                         "   AND    table_name like '%" + getName() + "%' and length(table_name) = length(?)\n" +
                         ")";
            return jdbcTemplate.queryForBoolean(sql, getSchema().getName(), getName());
        }
    }

    @Override
    protected void doLock() throws SQLException {
        LOG.debug("Unable to lock " + this + " as CockroachDB does not support locking. " +
                  "No concurrent migration supported.");
    }

    @Override
    protected void doUnlock() {
    }

    @Override
    public CockroachDBSchema getSchema() {
        return (CockroachDBSchema) super.getSchema();
    }
}
