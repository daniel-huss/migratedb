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
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.SQLException;
import java.util.List;

public abstract class BaseSchema implements Schema {
    private static final Log LOG = Log.getLog(BaseSchema.class);
    protected final JdbcTemplate jdbcTemplate;
    private final Database database;
    protected final String name;

    /**
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    public BaseSchema(JdbcTemplate jdbcTemplate, Database database, String name) {
        this.jdbcTemplate = jdbcTemplate;
        this.database = database;
        this.name = name;
    }

    protected Database getDatabase() {
        return database;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final boolean exists() {
        try {
            return doExists();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to check whether schema " + this + " exists", e);
        }
    }

    /**
     * Checks whether this schema exists.
     *
     * @throws SQLException when the check failed.
     */
    protected abstract boolean doExists() throws SQLException;

    @Override
    public final boolean isEmpty() {
        try {
            return doCheckIfEmpty();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to check whether schema " + this + " is empty", e);
        }
    }

    /**
     * Checks whether this schema is empty.
     *
     * @throws SQLException when the check failed.
     */
    protected abstract boolean doCheckIfEmpty() throws SQLException;

    @Override
    public final void create() {
        try {
            LOG.info("Creating schema " + this + " ...");
            doCreate();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to create schema " + this, e);
        }
    }

    /**
     * Creates this schema in the database.
     *
     * @throws SQLException when the creation failed.
     */
    protected abstract void doCreate() throws SQLException;

    @Override
    public final List<? extends Table> allTables() {
        try {
            return doAllTables();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to retrieve all tables in schema " + this, e);
        }
    }

    /**
     * Retrieves all the tables in this schema.
     *
     * @throws SQLException when the retrieval failed.
     */
    protected abstract List<? extends Table> doAllTables() throws SQLException;

    /**
     * @return The quoted name of this schema.
     */
    @Override
    public final String toString() {
        return getDatabase().quote(name);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseSchema other = (BaseSchema) o;
        return name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }
}
