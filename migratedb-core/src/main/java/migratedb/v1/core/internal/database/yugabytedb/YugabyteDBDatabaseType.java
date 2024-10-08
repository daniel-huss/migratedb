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
package migratedb.v1.core.internal.database.yugabytedb;

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.database.postgresql.PostgreSQLDatabaseType;
import migratedb.v1.core.internal.parser.BaseParser;

import java.sql.Connection;
import java.util.regex.Pattern;

public class YugabyteDBDatabaseType extends PostgreSQLDatabaseType {
    @Override
    public String getName() {
        return "YugabyteDB";
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
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new YugabyteDBDatabase(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new YugabyteDBParser(configuration, parsingContext);
    }
}
