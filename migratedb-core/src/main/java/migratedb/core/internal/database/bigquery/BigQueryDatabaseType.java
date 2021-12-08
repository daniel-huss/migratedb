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
package migratedb.core.internal.database.bigquery;

import java.sql.Connection;
import java.sql.Types;
import java.util.Map;
import java.util.regex.Pattern;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.database.base.Database;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.util.ClassUtils;

public class BigQueryDatabaseType extends BaseDatabaseType {
    private static final String BIGQUERY_JDBC42_DRIVER = "com.simba.googlebigquery.jdbc42.Driver";
    private static final String BIGQUERY_JDBC_DRIVER = "com.simba.googlebigquery.jdbc.Driver";
    private static final Pattern OAUTH_CREDENTIALS_PATTERN = Pattern.compile("OAuth\\w+=([^;]*)",
                                                                             Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "BigQuery";
    }

    @Override
    public int getNullType() {
        return Types.NULL;
    }

    @Override
    public int getPriority() {
        // All regular database types (including non-beta BigQuery support) take priority over this beta
        return -1;
    }

    @Override
    public Pattern getJDBCCredentialsPattern() {
        return OAUTH_CREDENTIALS_PATTERN;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:bigquery:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return BIGQUERY_JDBC42_DRIVER;
    }

    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        if (ClassUtils.isPresent(BIGQUERY_JDBC_DRIVER, classLoader)) {
            return BIGQUERY_JDBC_DRIVER;
        }
        return null;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        // https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers
        // databaseProductName: Google BigQuery 2.0, databaseProductVersion: 2.0
        return databaseProductName.toLowerCase().contains("bigquery");
    }

    @Override
    public void setOverridingConnectionProps(Map<String, String> props) {
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new BigQueryDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public Parser createParser(Configuration configuration, ResourceProvider resourceProvider,
                               ParsingContext parsingContext) {
        return new BigQueryParser(configuration, parsingContext);
    }

    @Override
    public String instantiateClassExtendedErrorMessage() {
        return "Failure probably due to inability to load dependencies. Please ensure you have downloaded " +
               "'https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers' and extracted to " +
               "'migratedb/drivers' folder";
    }
}
