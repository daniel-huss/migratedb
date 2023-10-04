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
package migratedb.v1.core.internal.database.base;

import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.jdbc.JdbcUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseTable extends BaseSchemaObject implements Table {
    /**
     * Keeps track of the locks on a table since calls to lock the table can be nested.
     */
    protected int lockDepth = 0;

    /**
     * @param jdbcTemplate The JDBC template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    public BaseTable(JdbcTemplate jdbcTemplate, Database database, Schema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    public final boolean exists() {
        try {
            return doExists();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to check whether table " + this + " exists", e);
        }
    }

    /**
     * Checks whether this table exists.
     *
     * @throws SQLException when the check failed.
     */
    protected abstract boolean doExists() throws SQLException;

    /**
     * Checks whether the database contains a table matching the given criteria.
     *
     * @param catalog    The catalog where the table resides. (optional)
     * @param schema     The schema where the table resides. (optional)
     * @param table      The name of the table. (optional)
     * @param tableTypes The types of table to look for (e.g. TABLE). (optional)
     * @throws SQLException when the check failed.
     */
    protected boolean exists(@Nullable Schema catalog,
                             @Nullable Schema schema,
                             String table,
                             String... tableTypes) throws SQLException {
        String[] types = tableTypes;
        if (types.length == 0) {
            types = null;
        }

        ResultSet resultSet = null;
        boolean found;
        try {
            resultSet = getDatabase().getJdbcMetaData().getTables(
                    catalog == null ? null : catalog.getName(),
                    schema == null ? null : getSchema().getName(),
                    table,
                    types);
            found = resultSet.next();
        } finally {
            JdbcUtils.closeResultSet(resultSet);
        }

        return found;
    }

    @Override
    public void lock() {
        if (!exists()) {
            return;
        }
        try {
            doLock();
            lockDepth++;
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to lock table " + this, e);
        }
    }

    /**
     * Locks this table in this schema using a read/write pessimistic lock until the end of the current transaction.
     * Note that {@code unlock()} still needs to be called even if your database unlocks the table implicitly (in which
     * case {@code doUnlock()} may be a no-op) in order to maintain the lock count correctly.
     *
     * @throws SQLException when this table in this schema could not be locked.
     */
    protected abstract void doLock() throws SQLException;

    @Override
    public void unlock() {
        // lockDepth can be zero if this table didn't exist at the time of the call to lock()
        if (!exists() || lockDepth == 0) {
            return;
        }
        try {
            doUnlock();
            lockDepth--;
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to unlock table " + this, e);
        }
    }

    /**
     * For databases that require an explicit unlocking, not an implicit end-of-transaction one.
     *
     * @throws SQLException when this table in this schema could not be unlocked.
     */
    protected void doUnlock() throws SQLException {
    }
}
