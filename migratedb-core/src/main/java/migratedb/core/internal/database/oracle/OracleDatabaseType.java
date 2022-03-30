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
package migratedb.core.internal.database.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutor;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.util.ClassUtils;
import oracle.jdbc.OracleConnection;

public class OracleDatabaseType extends BaseDatabaseType {
    // Oracle usernames/passwords can be 1-30 chars, can only contain alphanumerics and # _ $
    // The first (and only) capture group represents the password
    private static final Pattern usernamePasswordPattern = Pattern.compile(
        "^jdbc:oracle:thin:[a-zA-Z0-9#_$]+/([a-zA-Z0-9#_$]+)@.*");

    @Override
    public String getName() {
        return "Oracle";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:oracle") || url.startsWith("jdbc:p6spy:oracle");
    }

    @Override
    public Pattern getJDBCCredentialsPattern() {
        return usernamePasswordPattern;
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:oracle:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName.startsWith("Oracle");
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        OracleDatabase.enableTnsnamesOraSupport();

        return new OracleDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public BaseParser createParser(Configuration configuration, ResourceProvider resourceProvider,
                                   ParsingContext parsingContext) {

        return new OracleParser(configuration

            , parsingContext
        );
    }

    @Override
    public SqlScriptExecutorFactory createSqlScriptExecutorFactory(JdbcConnectionFactory jdbcConnectionFactory,
                                                                   CallbackExecutor callbackExecutor,
                                                                   StatementInterceptor statementInterceptor
    ) {

        DatabaseType thisRef = this;

        return new SqlScriptExecutorFactory() {
            @Override
            public SqlScriptExecutor createSqlScriptExecutor(Connection connection, boolean batch,
                                                             boolean outputQueryResults) {

                return new OracleSqlScriptExecutor(new JdbcTemplate(connection, thisRef)
                    , callbackExecutor, batch, outputQueryResults, statementInterceptor
                );
            }
        };
    }

    @Override
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        String osUser = System.getProperty("user.name");
        props.put("v$session.osuser", osUser.substring(0, Math.min(osUser.length(), 30)));
        props.put("v$session.program", APPLICATION_NAME);
        props.put("oracle.net.keepAlive", "true");

        String oobb = ClassUtils.getStaticFieldValue("oracle.jdbc.OracleConnection",
                                                     "CONNECTION_PROPERTY_THIN_NET_DISABLE_OUT_OF_BAND_BREAK",
                                                     classLoader);
        props.put(oobb, "true");
    }

    @Override
    public void setConfigConnectionProps(Configuration config, Properties props, ClassLoader classLoader) {

    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !usernamePasswordPattern.matcher(url).matches();
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {

        return !usernamePasswordPattern.matcher(url).matches();
    }

    @Override
    public Connection alterConnectionAsNeeded(Connection connection, Configuration configuration) {
        Map<String, String> jdbcProperties = configuration.getJdbcProperties();

        if (jdbcProperties != null && jdbcProperties.containsKey(OracleConnection.PROXY_USER_NAME)) {
            try {
                OracleConnection oracleConnection;

                try {
                    if (connection instanceof OracleConnection) {
                        oracleConnection = (OracleConnection) connection;
                    } else if (connection.isWrapperFor(OracleConnection.class)) {
                        // This includes com.zaxxer.HikariCP.HikariProxyConnection, potentially other unknown wrapper
                        // types
                        oracleConnection = connection.unwrap(OracleConnection.class);
                    } else {
                        throw new MigrateDbException(
                            "Unable to extract Oracle connection type from '" + connection.getClass().getName() + "'");
                    }
                } catch (SQLException e) {
                    throw new MigrateDbException(
                        "Unable to unwrap connection type '" + connection.getClass().getName() + "'", e);
                }

                if (!oracleConnection.isProxySession()) {
                    Properties props = new Properties();
                    props.putAll(configuration.getJdbcProperties());
                    oracleConnection.openProxySession(OracleConnection.PROXYTYPE_USER_NAME, props);
                }
            } catch (MigrateDbException e) {
                LOG.warn(e.getMessage());
            } catch (SQLException e) {
                throw new MigrateDbException("Unable to open proxy session: " + e.getMessage(), e);
            }
        }

        return super.alterConnectionAsNeeded(connection, configuration);
    }

}
