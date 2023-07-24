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

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.v1.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.Parser;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;

import java.sql.Connection;

/**
 * Extension point for supported database types. Instances are unmodifiable.
 */
public interface DatabaseType {
    /**
     * @return The human-readable name for this database type.
     */
    String getName();

    /**
     * @return The JDBC type used to represent {@code null} in prepared statements.
     */
    int getNullType();

    /**
     * When identifying database types, the priority with which this type will be used. High numbers indicate that this
     * type will be used in preference to others.
     */
    int getPriority();

    /**
     * Check if this database type handles the connection product name and version. This allows more fine-grained
     * control over which DatabaseType handles which connection. MigrateDB will use the first DatabaseType that returns
     * true for this method.
     *
     * @param databaseProductName    The product name returned by the database.
     * @param databaseProductVersion The product version returned by the database.
     * @param connection             The connection used to connect to the database.
     * @return {@code true} if this handles the product name and version, {@code false} if not.
     */
    boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                 Connection connection);

    /**
     * Initializes the Database class, and optionally prints some information.
     *
     * @param configuration         The MigrateDB configuration.
     * @param jdbcConnectionFactory The current connection factory.
     * @param printInfo             Where the DB info should be printed in the logs.
     * @return The appropriate Database class.
     */
    Database<?> createDatabase(Configuration configuration,
                               boolean printInfo,
                               JdbcConnectionFactory jdbcConnectionFactory);

    /**
     * Initializes the Database used by this Database Type.
     *
     * @param configuration         The MigrateDB configuration.
     * @param jdbcConnectionFactory The current connection factory.
     * @return The Database.
     */
    Database<?> createDatabase(Configuration configuration,
                               JdbcConnectionFactory jdbcConnectionFactory);

    /**
     * Initializes the Parser used by this Database Type.
     *
     * @param configuration The MigrateDB configuration.
     * @return The Parser.
     */
    Parser createParser(Configuration configuration, ResourceProvider resourceProvider, ParsingContext parsingContext);

    /**
     * Initializes the SqlScriptFactory used by this Database Type.
     *
     * @param configuration The MigrateDB configuration.
     * @return The SqlScriptFactory.
     */
    SqlScriptFactory createSqlScriptFactory(Configuration configuration,
                                            ParsingContext parsingContext);

    /**
     * Initializes the SqlScriptExecutorFactory used by this Database Type.
     *
     * @param jdbcConnectionFactory The current connection factory.
     * @return The SqlScriptExecutorFactory.
     */
    SqlScriptExecutorFactory createSqlScriptExecutorFactory(JdbcConnectionFactory jdbcConnectionFactory,
                                                            CallbackExecutor callbackExecutor);

    /**
     * Initializes the DatabaseExecutionStrategy used by this Database Type.
     *
     * @return The DatabaseExecutionStrategy.
     */
    DatabaseExecutionStrategy createExecutionStrategy(java.sql.Connection connection);

    /**
     * Initializes the ExecutionTemplate used by this Database Type.
     *
     * @return The ExecutionTemplate.
     */
    ExecutionTemplate createTransactionalExecutionTemplate(Connection connection, boolean rollbackOnException);

    /**
     * Carries out any manipulation on the Connection that is required by MigrateDB's config
     *
     * @param connection    The JDBC connection.
     * @param configuration The MigrateDB configuration.
     */
    void alterConnectionAsNeeded(Connection connection, Configuration configuration);
}
