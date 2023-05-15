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

package migratedb.core.api.internal.database.base;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.Version;
import migratedb.core.api.internal.sqlscript.Delimiter;

import java.sql.DatabaseMetaData;

public interface Database2 extends AutoCloseable {
    /**
     * Ensure MigrateDB supports this version of this database.
     *
     * @throws MigrateDbException If this database version is not supported.
     */
    void ensureSupported();

    /**
     * @return The 'major.minor' version of this database.
     */
    Version getVersion();

    Delimiter getDefaultDelimiter();

    /**
     * @return The name of the database, by default as determined by JDBC.
     */
    String getCatalog();

    String getCurrentUser();

    /**
     * @return {@code true} if this database uses a catalog to represent a schema, or {@code false} if a schema is
     * simply a schema.
     */
    boolean catalogIsSchema();

    /**
     * @return Whether to use a single connection for both schema history management and applying migrations.
     */
    boolean useSingleConnection();

    DatabaseMetaData getJdbcMetaData();

    /**
     * @return The main connection used to manipulate the schema history.
     */
    Connection2 getMainConnection();

    Store getStore();

    /**
     * @return The migration connection used to apply migrations.
     */
    Connection2 getMigrationConnection();

    DatabaseType getDatabaseType();

    boolean supportsEmptyMigrationDescription();

    boolean supportsMultiStatementTransactions();

    boolean supportsDdlTransactions();

    boolean supportsChangingCurrentSchema();

    @Override
    void close();
}
