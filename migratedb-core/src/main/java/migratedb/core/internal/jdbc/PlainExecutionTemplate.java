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

import java.sql.SQLException;
import java.util.concurrent.Callable;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.exception.MigrateDbSqlException;

public class PlainExecutionTemplate implements ExecutionTemplate {
    private static final Log LOG = Log.getLog(PlainExecutionTemplate.class);

    @Override
    public <T> T execute(Callable<T> callback) {
        try {
            LOG.debug("Performing operation in non-transactional context.");
            return callback.call();
        } catch (Exception e) {
            LOG.error(
                "Failed to execute operation in non-transactional context. Please restore backups and roll back " +
                "database and code!");

            if (e instanceof SQLException) {
                throw new MigrateDbSqlException("Failed to execute operation.", (SQLException) e);
            }

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }

            throw new MigrateDbException(e);
        }
    }
}
