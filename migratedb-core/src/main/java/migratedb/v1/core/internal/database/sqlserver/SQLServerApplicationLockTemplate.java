/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2024 The MigrateDB contributors
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
package migratedb.v1.core.internal.database.sqlserver;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Spring-like template for executing with SQL Server application locks.
 */
public class SQLServerApplicationLockTemplate {
    private static final Log LOG = Log.getLog(SQLServerApplicationLockTemplate.class);

    private final SQLServerSession session;
    private final JdbcTemplate jdbcTemplate;
    private final String databaseName;
    private final String lockName;

    /**
     * Creates a new application lock template for this connection.
     *
     * @param session       The current session.
     * @param jdbcTemplate  The jdbcTemplate for the connection.
     * @param discriminator A number to discriminate between locks.
     */
    SQLServerApplicationLockTemplate(SQLServerSession session, JdbcTemplate jdbcTemplate, String databaseName,
                                     int discriminator) {
        this.session = session;
        this.jdbcTemplate = jdbcTemplate;
        this.databaseName = databaseName;
        lockName = "MigrateDb-" + discriminator;
    }

    /**
     * Executes this callback with an advisory lock.
     *
     * @param callable The callback to execute.
     * @return The result of the callable code.
     */
    public <T> T execute(Callable<T> callable) {
        try {
            session.setCurrentDatabase(databaseName);
            jdbcTemplate.execute("EXEC sp_getapplock @Resource = ?, @LockTimeout='3600000'," +
                                 " @LockMode = 'Exclusive', @LockOwner = 'Session'", lockName);
            return callable.call();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to acquire SQL Server application lock", e);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            RuntimeException rethrow;
            if (e instanceof RuntimeException) {
                rethrow = (RuntimeException) e;
            } else {
                rethrow = new MigrateDbException(e);
            }
            throw rethrow;
        } finally {
            try {
                session.setCurrentDatabase(databaseName);
                jdbcTemplate.execute("EXEC sp_releaseapplock @Resource = ?, @LockOwner = 'Session'", lockName);
            } catch (SQLException e) {
                LOG.error("Unable to release SQL Server application lock", e);
            }
        }
    }
}
