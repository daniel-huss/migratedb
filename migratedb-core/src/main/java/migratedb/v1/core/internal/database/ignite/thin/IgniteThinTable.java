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
package migratedb.v1.core.internal.database.ignite.thin;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.internal.jdbc.Results;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseTable;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Ignite Thin-specific table.
 */
public class IgniteThinTable extends BaseTable {
    private static final Log LOG = Log.getLog(IgniteThinTable.class);

    private final String tableLockString = UUID.randomUUID().toString();

    /**
     * Creates a new Ignite table.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    public IgniteThinTable(JdbcTemplate jdbcTemplate, IgniteThinDatabase database, IgniteThinSchema schema,
                           String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return exists(null, getSchema(), getName());
    }

    @Override
    protected void doLock() throws SQLException {
        if (lockDepth > 0) {
            // Lock has already been taken - so the relevant row in the table already exists
            return;
        }
        int retryCount = 0;
        do {
            try {
                if (insertLockingRow()) {
                    return;
                }
                retryCount++;
                LOG.debug("Waiting for lock on " + this);
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // Ignore - if interrupted, we still need to wait for lock to become available
            }
        } while (retryCount < 50);
        throw new MigrateDbException("Unable to obtain table lock - another MigrateDb instance may be running");
    }

    @Override
    protected void doUnlock() throws SQLException {
        // Leave the locking row alone until we get to the final level of unlocking
        if (lockDepth > 1) {
            return;
        }
        // Check that there are no other locks in place. This should not happen!
        int competingLocksTaken = jdbcTemplate.queryForInt(
                "SELECT COUNT(*) FROM " + this + " WHERE " + getDatabase().quote("version") + " != '" + tableLockString +
                "' AND " +
                getDatabase().quote("description") + " = 'migratedb-lock'");
        if (competingLocksTaken > 0) {
            throw new MigrateDbException("Internal error: on unlocking, a competing lock was found");
        }
        // Remove the locking row
        jdbcTemplate.executeStatement(
                "DELETE FROM " + this + " WHERE " + getDatabase().quote("version") + " = '" + tableLockString + "' AND " +
                getDatabase().quote("description") + " = 'migratedb-lock'");
    }

    private boolean insertLockingRow() {
        // Insert the locking row - the primary keyness of installed_rank will prevent us having two.
        Results results = jdbcTemplate.executeStatement("INSERT INTO " + this + " VALUES (-100, '" + tableLockString +
                                                        "', 'migratedb-lock', '', '', 0, '', now(), 0, TRUE)");
        // Succeeded if no errors.
        return results.getException() == null;
    }
}
