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
package migratedb.v1.core.internal.database.base;

import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Function;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;
import migratedb.v1.core.internal.jdbc.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class BaseSchema<D extends Database<?>, T extends Table<?, ?>> implements Schema<D, T> {
    private static final Log LOG = Log.getLog(BaseSchema.class);
    protected final JdbcTemplate jdbcTemplate;
    protected final D database;
    protected final String name;

    /**
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    public BaseSchema(JdbcTemplate jdbcTemplate, D database, String name) {
        this.jdbcTemplate = jdbcTemplate;
        this.database = database;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean exists() {
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
    public boolean empty() {
        try {
            return doEmpty();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to check whether schema " + this + " is empty", e);
        }
    }

    /**
     * Checks whether this schema is empty.
     *
     * @throws SQLException when the check failed.
     */
    protected abstract boolean doEmpty() throws SQLException;

    @Override
    public void create() {
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
    public void drop() {
        try {
            doDrop();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to drop schema " + this, e);
        }
    }

    /**
     * Drops this schema from the database.
     *
     * @throws SQLException when the drop failed.
     */
    protected abstract void doDrop() throws SQLException;

    @Override
    public void clean() {
        try {
            doClean();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to clean schema " + this, e);
        }
    }

    /**
     * Cleans all the objects in this schema.
     *
     * @throws SQLException when the clean failed.
     */
    protected abstract void doClean() throws SQLException;

    @Override
    public T[] allTables() {
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
    protected abstract T[] doAllTables() throws SQLException;

    /**
     * Retrieves all the types in this schema.
     */
    protected final Type<?, ?>[] allTypes() {
        ResultSet resultSet = null;
        try {
            resultSet = database.getJdbcMetaData().getUDTs(null, name, null, null);

            var types = new ArrayList<Type<?, ?>>();
            while (resultSet.next()) {
                types.add(getType(resultSet.getString("TYPE_NAME")));
            }

            return types.toArray(Type<?, ?>[]::new);
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to retrieve all types in schema " + this, e);
        } finally {
            JdbcUtils.closeResultSet(resultSet);
        }
    }

    /**
     * Retrieves the type with this name in this schema.
     */
    protected Type<?, ?> getType(String typeName) {
        return null;
    }

    @Override
    public Function<?, ?> getFunction(String functionName, String... args) {
        throw new UnsupportedOperationException("getFunction()");
    }

    /**
     * Retrieves all the functions in this schema.
     */
    protected final Function<?, ?>[] allFunctions() {
        try {
            return doAllFunctions();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to retrieve all functions in schema " + this, e);
        }
    }

    /**
     * Retrieves all the functions in this schema.
     *
     * @throws SQLException when the retrieval failed.
     */
    protected Function<?, ?>[] doAllFunctions() throws SQLException {
        return new Function<?, ?>[0];
    }

    /**
     * @return The quoted name of this schema.
     */
    @Override
    public String toString() {
        return database.quote(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseSchema<?, ?> schema = (BaseSchema<?, ?>) o;
        return name.equals(schema.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
