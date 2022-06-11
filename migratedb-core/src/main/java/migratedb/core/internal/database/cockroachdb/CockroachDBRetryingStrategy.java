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
package migratedb.core.internal.database.cockroachdb;

import migratedb.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.core.api.internal.util.SqlCallable;
import migratedb.core.api.logging.Log;

import java.sql.SQLException;

/**
 * CockroachDB recommend the use of retries should we see a SQL error code 40001, which represents a lock wait timeout.
 * This class implements an appropriate retry pattern.
 */
public class CockroachDBRetryingStrategy implements DatabaseExecutionStrategy {
    private static final Log LOG = Log.getLog(CockroachDBRetryingStrategy.class);

    private static final String DEADLOCK_OR_TIMEOUT_ERROR_CODE = "40001";
    private static final int MAX_RETRIES = 50;

    @Override
    public <T> T execute(SqlCallable<T> callable) throws SQLException {
        int retryCount = 0;
        while (true) {
            try {
                return callable.call();
            } catch (SQLException e) {
                checkRetryOrThrow(e, retryCount);
                retryCount++;
            }
        }
    }

    void checkRetryOrThrow(SQLException e, int retryCount) throws SQLException {
        if (DEADLOCK_OR_TIMEOUT_ERROR_CODE.equals(e.getSQLState()) && retryCount < MAX_RETRIES) {
            LOG.info("Retrying because of deadlock or timeout: " + e.getMessage());
        }
        // Exception is non-retryable
        throw e;
    }
}
