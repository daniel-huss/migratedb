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
package migratedb.v1.core.internal.database.sybasease;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchemaObject;
import migratedb.v1.core.internal.database.base.BaseTable;

import java.sql.SQLException;

/**
 * Sybase ASE table.
 */
public class SybaseASETable extends BaseTable {
    /**
     * Creates a new SAP ASE table.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    SybaseASETable(JdbcTemplate jdbcTemplate, SybaseASEDatabase database, SybaseASESchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForString("SELECT object_id('" + getName() + "')") != null;
    }

    @Override
    protected void doLock() throws SQLException {
        // MigrateDB's locking assumes transactions are being used to release locks on commit at some later point
        // (hence the lack of an 'unlock' method)
        // If multi statement transactions aren't supported, then locking a table makes no sense,
        // since that's the only operation we can do
        if (getDatabase().supportsMultiStatementTransactions()) {
            jdbcTemplate.execute("LOCK TABLE " + this + " IN EXCLUSIVE MODE");
        }
    }

    /**
     * Since Sybase ASE does not support schema, dropping out the schema name for toString method
     *
     * @see BaseSchemaObject#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
}
