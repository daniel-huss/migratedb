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
package migratedb.core.internal.database.yugabytedb;

import java.sql.Connection;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.database.postgresql.PostgreSQLDatabase;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;

public class YugabyteDBDatabase extends PostgreSQLDatabase {

    public YugabyteDBDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                              StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected YugabyteDBConnection doGetConnection(Connection connection) {
        return new YugabyteDBConnection(configuration, this, connection);
    }

    @Override
    public void ensureSupported() {
        // Checks the Postgres version
        ensureDatabaseIsRecentEnough("11.2");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

}
