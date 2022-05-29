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
package migratedb.core.internal.database.cockroachdb;

import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.jdbc.ExecutionTemplate;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.authentication.postgres.PgpassFileReader;
import migratedb.core.internal.database.DefaultExecutionStrategy;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.util.Development;

import java.sql.Connection;
import java.sql.Types;
import java.util.Properties;

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
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:p6spy:postgresql:");
    }

    @Override
    public int getPriority() {
        // Must be checked ahead of the vanilla PostgreSQLDatabaseType
        return 1;
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:postgresql:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.postgresql.Driver";
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
    public Database<?> createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
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

    @Override
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("applicationName", APPLICATION_NAME);
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !url.contains("user=");
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        // Postgres supports password in URL
        return !url.contains("password=");
    }

    @Override
    public boolean externalAuthPropertiesRequired(String url, String username, String password) {

        return super.externalAuthPropertiesRequired(url, username, password);

    }

    @Override
    public Properties getExternalAuthProperties(String url, String username) {
        PgpassFileReader pgpassFileReader = new PgpassFileReader();

        if (pgpassFileReader.getPgpassFilePath() != null) {
            Development.TODO("Support PGPASSFILE password file?");
        }
        return super.getExternalAuthProperties(url, username);

    }
}
