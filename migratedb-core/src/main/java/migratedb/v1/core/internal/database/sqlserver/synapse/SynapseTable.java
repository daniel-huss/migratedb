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
package migratedb.v1.core.internal.database.sqlserver.synapse;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.sqlserver.SQLServerDatabase;
import migratedb.v1.core.internal.database.sqlserver.SQLServerSchema;
import migratedb.v1.core.internal.database.sqlserver.SQLServerTable;

import java.sql.SQLException;

public class SynapseTable extends SQLServerTable {
    private static final Log LOG = Log.getLog(SynapseTable.class);

    SynapseTable(JdbcTemplate jdbcTemplate, SQLServerDatabase database, String databaseName, SQLServerSchema schema,
                 String name) {
        super(jdbcTemplate, database, databaseName, schema, name);
    }

    @Override
    protected void doLock() throws SQLException {
        LOG.debug("Unable to lock " + this + " as Synapse does not support locking. " +
                  "No concurrent migration supported.");
    }

    @Override
    protected void doUnlock() {
    }
}
