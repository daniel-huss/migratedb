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
package migratedb.core.internal.database.base;

import java.sql.Connection;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.ParsingContext;

public class TestContainersDatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "Test Containers";
    }

    @Override
    public int getNullType() {
        return 0;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:tc:") || url.startsWith("jdbc:p6spy:tc:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:tc:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.testcontainers.jdbc.ContainerDatabaseDriver";
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !url.contains("user=");
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !url.contains("password=");
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return false;
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        throw new IllegalStateException();
    }

    @Override
    public Parser createParser(Configuration configuration, ResourceProvider resourceProvider,
                               ParsingContext parsingContext) {
        throw new IllegalStateException();
    }
}
