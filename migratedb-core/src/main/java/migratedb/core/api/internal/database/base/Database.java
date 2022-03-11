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

import java.io.Closeable;
import java.sql.DatabaseMetaData;
import migratedb.core.api.MigrationVersion;
import migratedb.core.internal.sqlscript.Delimiter;
import migratedb.core.internal.sqlscript.SqlScript;
import migratedb.core.internal.sqlscript.SqlScriptFactory;

public interface Database<C extends Connection> extends Closeable {
    /**
     * Ensure MigrateDb supports this version of this database.
     */
    void ensureSupported();

    /**
     * @return The 'major.minor' version of this database.
     */
    MigrationVersion getVersion();

    Delimiter getDefaultDelimiter();

    /**
     * @return The name of the database, by default as determined by JDBC.
     */
    String getCatalog();

    String getCurrentUser();

    boolean supportsDdlTransactions();

    boolean supportsChangingCurrentSchema();

    /**
     * @return The representation of the value {@code true} in a boolean column.
     */
    String getBooleanTrue();

    /**
     * @return The representation of the value {@code false} in a boolean column.
     */
    String getBooleanFalse();

    /**
     * Quotes these identifiers for use in SQL queries. Multiple identifiers will be quoted and separated by a dot.
     */
    String quote(String... identifiers);

    /**
     * @return {@code true} if this database uses a catalog to represent a schema, or {@code false} if a schema is
     * simply a schema.
     */
    boolean catalogIsSchema();

    /**
     * @return Whether to use a single connection for both schema history table management and applying migrations.
     */
    boolean useSingleConnection();

    DatabaseMetaData getJdbcMetaData();

    /**
     * @return The main connection used to manipulate the schema history.
     */
    C getMainConnection();

    /**
     * @return The migration connection used to apply migrations.
     */
    C getMigrationConnection();

    /**
     * Retrieves the script used to create the schema history table.
     *
     * @param sqlScriptFactory The factory used to create the SQL script.
     * @param table            The table to create.
     * @param baseline         Whether to include the creation of a baseline marker.
     */
    SqlScript getCreateScript(SqlScriptFactory sqlScriptFactory, Table table, boolean baseline);

    String getRawCreateScript(Table table, boolean baseline);

    String getInsertStatement(Table table);

    String getBaselineStatement(Table table);

    String getSelectStatement(Table table);

    String getInstalledBy();

    DatabaseType getDatabaseType();

    boolean supportsEmptyMigrationDescription();

    boolean supportsMultiStatementTransactions();

    /**
     * Cleans all the objects in this database that need to be cleaned before each schema.
     */
    void cleanPreSchemas();

    /**
     * Cleans all the objects in this database that need to be cleaned after each schema.
     *
     * @param schemas The list of schemas managed by MigrateDb.
     */
    void cleanPostSchemas(Schema[] schemas);

    Schema[] getAllSchemas();

    @Override
    void close();
}
