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
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.SchemaObject;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.exception.MigrateDbSqlException;

import java.sql.SQLException;

public abstract class BaseSchemaObject<D extends Database<?>, S extends Schema<?, ?>> implements SchemaObject<D, S> {
    protected final JdbcTemplate jdbcTemplate;
    protected final D database;
    protected final S schema;
    protected final String name;

    /**
     * @param jdbcTemplate The JDBC template to access the DB.
     * @param database     The database-specific support.
     * @param schema       The schema the object lives in.
     * @param name         The name of the object.
     */
    BaseSchemaObject(JdbcTemplate jdbcTemplate, D database, S schema, String name) {
        this.name = name;
        this.jdbcTemplate = jdbcTemplate;
        this.database = database;
        this.schema = schema;
    }

    @Override
    public final D getDatabase() {
        return database;
    }

    @Override
    public final S getSchema() {
        return schema;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void drop() {
        try {
            doDrop();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to drop " + this, e);
        }
    }

    /**
     * @throws java.sql.SQLException when the drop failed.
     */
    protected abstract void doDrop() throws SQLException;

    @Override
    public String toString() {
        return database.quote(schema.getName(), name);
    }
}
