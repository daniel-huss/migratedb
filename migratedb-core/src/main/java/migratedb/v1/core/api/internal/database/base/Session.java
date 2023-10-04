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

package migratedb.v1.core.api.internal.database.base;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;

import java.util.concurrent.Callable;

public interface Session extends AutoCloseable {
    /**
     * Retrieves the current schema, which is the default owner of new database objects. In many databases, the current
     * schema is also used to lookup tables whenever the schema qualifier is omitted.
     */
    Schema getCurrentSchema();

    /**
     * Retrieves the schema with this name in the database.
     */
    Schema getSchema(String name);

    /**
     * Sets the current schema, which is the default owner of new database objects. In many databases, the current
     * schema is also used to lookup tables whenever the schema qualifier is omitted.
     */
    void changeCurrentSchemaTo(Schema schema);

    /**
     * Locks this table and executes this callable.
     *
     * @return The result of the callable.
     */
    <T> T lock(Table table, Callable<T> callable);

    JdbcTemplate getJdbcTemplate();

    /**
     * Undoes all changes to database settings and variables whose scope is bound to the lifetime of the JDBC
     * connection. An example of such state is the {@code search_path} setting of PostgreSQL. Methods that may change
     * state are:
     * <ul>
     *     <li>{@link #changeCurrentSchemaTo(Schema)}</li>
     *     <li>{@link #lock(Table, Callable)}</li>
     * </ul>
     * <p>
     * Will only undo changes that have been made through this object. Changes made by migration scripts are not
     * accounted for.
     */
    void restoreOriginalState();

    java.sql.Connection getJdbcConnection();

    @Override
    void close();
}
