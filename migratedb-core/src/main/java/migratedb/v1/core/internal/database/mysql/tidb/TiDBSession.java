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
package migratedb.v1.core.internal.database.mysql.tidb;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.internal.database.mysql.MySQLSession;
import migratedb.v1.core.internal.database.mysql.MySQLDatabase;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class TiDBSession extends MySQLSession {
    private static final String TIDB_TXN_MODE = "tidb_txn_mode";

    public TiDBSession(MySQLDatabase database, Connection connection) {
        super(database, connection);
    }

    private void setTidbTxnMode() {
        try {
            // Since we use a separate connection for the lock, it is not required to restore the transaction mode.
            jdbcTemplate.execute("SET " + TIDB_TXN_MODE + "=pessimistic");
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to set value for 'tidb_txn_mode' variable", e);
        }
    }

    @Override
    public <T> T lock(Table table, Callable<T> callable) {
        setTidbTxnMode();
        return super.lock(table, callable);
    }

    @Override
    protected boolean canUseNamedLockTemplate() {
        return false;
    }
}
