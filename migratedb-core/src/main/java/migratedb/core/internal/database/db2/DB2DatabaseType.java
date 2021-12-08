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
package migratedb.core.internal.database.db2;

import java.sql.Connection;
import java.sql.Types;
import java.util.Properties;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.database.base.Database;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.ParsingContext;

public class DB2DatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "DB2";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:db2:") || url.startsWith("jdbc:p6spy:db2:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:db2:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName.startsWith("DB2");
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new DB2Database(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public Parser createParser(Configuration configuration, ResourceProvider resourceProvider,
                               ParsingContext parsingContext) {
        return new DB2Parser(configuration, parsingContext);
    }

    @Override
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("clientProgramName", APPLICATION_NAME);
        props.put("retrieveMessagesFromServerOnGetMessage", "true");
    }
}
