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
package migratedb.core.internal.database.h2;

import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.parser.BaseParser;

import java.sql.Connection;
import java.sql.Types;
import java.util.Locale;

public class H2DatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "H2";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:h2:") || url.startsWith("jdbc:p6spy:h2:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:h2:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.h2.Driver";
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName.startsWith("H2");
    }

    @Override
    public Database<?> createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new H2Database(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new H2Parser(configuration, parsingContext);
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !(url.toLowerCase(Locale.ROOT).contains(":mem:"));
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !(url.toLowerCase(Locale.ROOT).contains(":mem:"));
    }
}
