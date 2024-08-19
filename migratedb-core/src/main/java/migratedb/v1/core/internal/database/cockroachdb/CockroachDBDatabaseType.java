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
package migratedb.v1.core.internal.database.cockroachdb;

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.database.DefaultExecutionStrategy;
import migratedb.v1.core.internal.database.base.BaseDatabaseType;
import migratedb.v1.core.internal.database.postgresql.PostgreSQLDatabase;
import migratedb.v1.core.internal.database.postgresql.PostgreSQLDatabaseType;
import migratedb.v1.core.internal.parser.BaseParser;

import java.sql.Connection;
import java.sql.Types;

public class CockroachDBDatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "CockroachDB";
    }

    @Override
    public int getNullType() {
        return Types.NULL;
    }

    @Override
    public int getPriority() {
        // Must be checked ahead of the vanilla PostgreSQLDatabaseType
        return PostgreSQLDatabaseType.PRIORITY + 1;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        if (databaseProductName.startsWith("PostgreSQL")) {
            String selectVersionQueryOutput = getSelectVersionOutput(connection);
            return selectVersionQueryOutput.contains("CockroachDB");
        }

        return false;
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new CockroachDBDatabase(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new CockroachDBParser(configuration, parsingContext);
    }

    @Override
    public DatabaseExecutionStrategy createExecutionStrategy(Connection connection) {
        if (connection == null) {
            return new DefaultExecutionStrategy();
        }

        return new CockroachDBRetryingStrategy();
    }

    @Override
    public ExecutionTemplate createTransactionalExecutionTemplate(Connection connection, boolean rollbackOnException) {
        return new CockroachRetryingTransactionalExecutionTemplate(connection, rollbackOnException);
    }
}
