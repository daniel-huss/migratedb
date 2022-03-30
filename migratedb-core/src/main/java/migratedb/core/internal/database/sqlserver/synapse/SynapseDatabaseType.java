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
package migratedb.core.internal.database.sqlserver.synapse;

import java.sql.Connection;
import java.sql.SQLException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.database.sqlserver.SQLServerDatabaseType;
import migratedb.core.internal.database.sqlserver.SQLServerEngineEdition;
import migratedb.core.internal.exception.MigrateDbSqlException;

public class SynapseDatabaseType extends SQLServerDatabaseType {
    @Override
    protected boolean supportsJTDS() {
        return false;
    }

    @Override
    public String getName() {
        return "Azure Synapse";
    }

    @Override
    public int getPriority() {
        // Synapse needs to be checked in advance of the plain SQL Server type
        return 1;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        if (databaseProductName.startsWith("Microsoft SQL Server")) {

            try {
                SQLServerEngineEdition engineEdition =
                    SQLServerEngineEdition.fromCode(getJdbcTemplate(connection).queryForInt(
                    "SELECT SERVERPROPERTY('engineedition')"));
                return engineEdition == SQLServerEngineEdition.SQL_DATA_WAREHOUSE;
            } catch (SQLException e) {
                throw new MigrateDbSqlException("Unable to determine database engine edition.'", e);
            }
        }

        return false;
    }

    private JdbcTemplate getJdbcTemplate(Connection connection) {
        return new JdbcTemplate(connection, this);
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new SynapseDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }
}
