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
package migratedb.core.internal.jdbc;

import java.sql.Connection;
import migratedb.core.api.DatabaseTypeRegister;
import migratedb.core.internal.database.DatabaseType;
import migratedb.core.internal.database.base.Database;
import migratedb.core.internal.database.base.Table;

public class ExecutionTemplateFactory {
    /**
     * Creates a new execution template for this connection. If possible, will attempt to roll back when an exception is
     * thrown.
     *
     * @param connection The connection for execution.
     */
    public static ExecutionTemplate createExecutionTemplate(Connection connection,
                                                            DatabaseTypeRegister databaseTypeRegister) {
        return createTransactionalExecutionTemplate(connection,
                                                    true,
                                                    databaseTypeRegister.getDatabaseTypeForConnection(connection));
    }

    /**
     * Creates a new execution template for this connection. If possible, will attempt to roll back when an exception is
     * thrown.
     *
     * @param connection The connection for execution.
     * @param database   The database
     */
    public static ExecutionTemplate createExecutionTemplate(Connection connection, Database database) {
        if (database.supportsMultiStatementTransactions()) {
            return createTransactionalExecutionTemplate(connection, true, database.getDatabaseType());
        }

        return new PlainExecutionTemplate();
    }

    /**
     * Creates a new execution template for this connection, which attempts to get exclusive access to the table
     *
     * @param connection The connection for execution.
     * @param database   The database
     */
    public static ExecutionTemplate createTableExclusiveExecutionTemplate(Connection connection, Table table,
                                                                          Database database) {
        if (database.supportsMultiStatementTransactions()) {
            return new TableLockingExecutionTemplate(table,
                                                     createTransactionalExecutionTemplate(connection,
                                                                                          database.supportsDdlTransactions(),
                                                                                          database.getDatabaseType()));
        }

        return new TableLockingExecutionTemplate(table, new PlainExecutionTemplate());
    }

    /**
     * Creates a new transactional execution template for this connection.
     *
     * @param connection          The connection for execution.
     * @param rollbackOnException Whether to attempt to roll back when an exception is thrown.
     */
    private static ExecutionTemplate createTransactionalExecutionTemplate(Connection connection,
                                                                          boolean rollbackOnException,
                                                                          DatabaseType databaseType) {
        return databaseType.createTransactionalExecutionTemplate(connection, rollbackOnException);
    }
}
