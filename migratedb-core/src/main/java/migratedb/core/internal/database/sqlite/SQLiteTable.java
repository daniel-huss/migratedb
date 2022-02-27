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
package migratedb.core.internal.database.sqlite;

import java.sql.SQLException;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.base.BaseTable;

/**
 * SQLite-specific table.
 */
public class SQLiteTable extends BaseTable<SQLiteDatabase, SQLiteSchema> {
    private static final Log LOG = Log.getLog(SQLiteTable.class);

    /**
     * SQLite system tables are undroppable.
     */
    static final String SQLITE_SEQUENCE = "sqlite_sequence";
    private final boolean undroppable;

    /**
     * Creates a new SQLite table.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    public SQLiteTable(JdbcTemplate jdbcTemplate, SQLiteDatabase database, SQLiteSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
        undroppable = SQLITE_SEQUENCE.equals(name);
    }

    @Override
    protected void doDrop() throws SQLException {
        if (undroppable) {
            LOG.debug("SQLite system table " + this + " cannot be dropped. Ignoring.");
        } else {
            String dropSql = "DROP TABLE " + database.quote(schema.getName(), name);
            if (getSchema().getForeignKeysEnabled()) {
                // #2417: Disable foreign keys before dropping tables to avoid constraint violation errors
                dropSql = "PRAGMA foreign_keys = OFF; " + dropSql + "; PRAGMA foreign_keys = ON";
            }
            jdbcTemplate.execute(dropSql);
        }
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT count(tbl_name) FROM "
                                        + database.quote(schema.getName()) +
                                        ".sqlite_master WHERE type='table' AND tbl_name='" + name + "'") > 0;
    }

    @Override
    protected void doLock() {
        LOG.debug("Unable to lock " + this + " as SQLite does not support locking. No concurrent migration supported.");
    }
}
