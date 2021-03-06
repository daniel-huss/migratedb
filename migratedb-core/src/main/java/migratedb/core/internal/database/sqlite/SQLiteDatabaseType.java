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
package migratedb.core.internal.database.sqlite;

import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.util.FeatureDetector;

import java.sql.Connection;
import java.sql.Types;

public class SQLiteDatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "SQLite";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:sqlite:") || url.startsWith("jdbc:sqldroid:") ||
               url.startsWith("jdbc:p6spy:sqlite:") || url.startsWith("jdbc:p6spy:sqldroid:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:sqlite:") || url.startsWith("jdbc:p6spy:sqldroid:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        if (url.startsWith("jdbc:sqldroid:")) {
            return "org.sqldroid.SQLDroidDriver";
        }
        if (new FeatureDetector(classLoader).isAndroidAvailable()) {
            return "org.sqldroid.SQLDroidDriver";
        }
        return "org.sqlite.JDBC";
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName.startsWith("SQLite");
    }

    @Override
    public Database<?> createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new SQLiteDatabase(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new SQLiteParser(configuration, parsingContext);
    }
}
