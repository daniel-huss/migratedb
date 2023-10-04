/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.internal.database.ignite.thin;

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.database.base.BaseDatabaseType;
import migratedb.v1.core.internal.parser.BaseParser;

import java.sql.Connection;
import java.sql.Types;

/*
 * # Apache Ignite Thin: jdbc:ignite:thin://<hostAndPortRange0>[,<hostAndPortRange1>]...[,
 * <hostAndPortRangeN>][/schema][?<params>] where hostAndPortRange := host[:port_from[..port_to]], params :=
 * param1=value1[&param2=value2]...[&paramN=valueN]
 *
 * See https://ignite.apache.org/docs/latest/installation/installing-using-docker
 *
 * For simple testing, use `docker run -d -p 10800:10800 apacheignite/ignite` to spin up the DB and then
 * `jdbc:ignite:thin://127.0.0.1` as the JDBC URL, no username or password
 * */
public class IgniteThinDatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "Apache Ignite";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName.startsWith("Apache Ignite");
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new IgniteThinDatabase(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new IgniteThinParser(configuration, parsingContext);
    }
}
