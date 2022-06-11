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
package migratedb.core.internal.database.mysql;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.exception.MigrateDbSqlException;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Spring-like template for executing with MySQL named locks.
 */
public class MySQLNamedLockTemplate {
    private static final Log LOG = Log.getLog(MySQLNamedLockTemplate.class);

    /**
     * The connection for the named lock.
     */
    private final JdbcTemplate jdbcTemplate;

    private final String lockName;

    /**
     * Creates a new named lock template for this connection.
     *
     * @param jdbcTemplate  The jdbcTemplate for the connection.
     * @param discriminator A number to discriminate between locks.
     */
    MySQLNamedLockTemplate(JdbcTemplate jdbcTemplate, int discriminator) {
        this.jdbcTemplate = jdbcTemplate;
        lockName = "MigrateDb-" + discriminator;
    }

    /**
     * Executes this callback with a named lock.
     *
     * @param callable The callback to execute.
     *
     * @return The result of the callable code.
     */
    public <T> T execute(Callable<T> callable) {
        try {
            lock();
            return callable.call();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to acquire MySQL named lock: " + lockName, e);
        } catch (Exception e) {
            RuntimeException rethrow;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e instanceof RuntimeException) {
                rethrow = (RuntimeException) e;
            } else {
                rethrow = new MigrateDbException(e);
            }
            throw rethrow;
        } finally {
            try {
                jdbcTemplate.execute("SELECT RELEASE_LOCK('" + lockName + "')");
            } catch (SQLException e) {
                LOG.error("Unable to release MySQL named lock: " + lockName, e);
            }
        }
    }

    private void lock() throws SQLException {
        while (!tryLock()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new MigrateDbException("Interrupted while attempting to acquire MySQL named lock: " + lockName,
                                             e);
            }
        }
    }

    private boolean tryLock() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT GET_LOCK(?,10)", lockName) == 1;
    }
}
