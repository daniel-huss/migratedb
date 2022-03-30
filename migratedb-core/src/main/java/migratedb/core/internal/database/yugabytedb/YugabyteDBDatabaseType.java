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
package migratedb.core.internal.database.yugabytedb;

import java.sql.Connection;
import java.util.regex.Pattern;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.database.postgresql.PostgreSQLDatabaseType;
import migratedb.core.internal.parser.BaseParser;

public class YugabyteDBDatabaseType extends PostgreSQLDatabaseType {
    @Override
    public String getName() {
        return "YugabyteDB";
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:p6spy:postgresql:");
    }

    @Override
    public int getPriority() {
        // Should be checked before plain PostgreSQL
        return 1;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        // The YB is what distinguishes Yugabyte
        return databaseProductName.startsWith("PostgreSQL")
               && Pattern.matches("PostgreSQL\\s\\d{1,2}(\\.\\d{1,2})?-YB-\\d{1,2}(\\.\\d{1,2})?.*",
                                  getSelectVersionOutput(connection));
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new YugabyteDBDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new YugabyteDBParser(configuration, parsingContext);
    }
}
