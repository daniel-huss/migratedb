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
package migratedb.v1.core.internal.database.redshift;

import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.database.base.BaseDatabaseType;
import migratedb.v1.core.internal.parser.BaseParser;
import migratedb.v1.core.internal.util.ClassUtils;

import java.sql.Connection;
import java.sql.Types;
import java.util.Map;

public class RedshiftDatabaseType extends BaseDatabaseType {
    @Override
    public String getName() {
        return "Redshift";
    }

    @Override
    public int getPriority() {
        // Redshift needs to be checked in advance of Postgres
        return 1;
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }


    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        if (databaseProductName.startsWith("PostgreSQL")) {
            String selectVersionQueryOutput = getSelectVersionOutput(connection);
            if (databaseProductName.startsWith("PostgreSQL 8") && selectVersionQueryOutput.contains("Redshift")) {
                return true;
            }
        }
        // This is the open-source driver at https://github.com/aws/amazon-redshift-jdbc-driver
        return databaseProductName.startsWith("Redshift");
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory) {
        return new RedshiftDatabase(configuration, jdbcConnectionFactory);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {
        return new RedshiftParser(configuration, parsingContext);
    }
}
