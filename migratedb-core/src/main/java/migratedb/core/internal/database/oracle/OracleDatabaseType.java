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
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.util.ClassUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.regex.Pattern;

public class OracleDatabaseType extends BaseDatabaseType {
    // Oracle usernames/passwords can be 1-30 chars, can only contain alphanumerics and # _ $
    // The first (and only) capture group represents the password
    private static final Pattern usernamePasswordPattern = Pattern.compile(
            "^jdbc:oracle:thin:[a-zA-Z\\d#_$]+/([a-zA-Z\\d#_$]+)@.*");

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
    public Database<?> createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
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

        return (connection, batch, outputQueryResults) -> new OracleSqlScriptExecutor(new JdbcTemplate(connection, thisRef)
                , callbackExecutor, batch, outputQueryResults, statementInterceptor
        );
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
        var accessor = new ReflectiveOracleAccessor(connection, configuration);
        if (accessor.isProxyUserNameConfigured()) {
            try {
                accessor.openProxySession();
            } catch (MigrateDbException e) {
                LOG.warn(e.getMessage());
            } catch (SQLException e) {
                throw new MigrateDbException("Unable to open proxy session: " + e.getMessage(), e);
            }
        }
        return super.alterConnectionAsNeeded(connection, configuration);
    }

    /**
     * Avoids a possibly conflicting dependency on the Oracle JDBC driver.
     */
    private static final class ReflectiveOracleAccessor {
        private static final String oracleClassName = "oracle.jdbc.OracleConnection";

        private final Connection connection;
        private final Configuration configuration;
        private final Class<?> oracleConnectionClass;

        ReflectiveOracleAccessor(Connection connection, Configuration configuration) {
            this.configuration = configuration;
            this.connection = connection;
            this.oracleConnectionClass = ClassUtils.loadClass(oracleClassName, configuration.getClassLoader());
        }

        private boolean isProxySession(Object oracleConnection) throws SQLException {
            var result = ClassUtils.invoke(oracleConnectionClass,
                                           "isProxySession",
                                           oracleConnection,
                                           new Class[0],
                                           new Object[0],
                                           e -> e instanceof SQLException ? (SQLException) e : null
            );
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new MigrateDbException("Expected boolean result, got " + ClassUtils.getClassName(result));
        }

        void openProxySession() throws SQLException {
            Object oracleConnection;
            if (oracleConnectionClass.isInstance(connection)) {
                oracleConnection = connection;
            } else if (connection.isWrapperFor(oracleConnectionClass)) {
                oracleConnection = connection.unwrap(oracleConnectionClass);
            } else {
                throw new MigrateDbException(
                    "Unable to extract Oracle connection type from '" + connection.getClass().getName() + "'");
            }

            if (!isProxySession(oracleConnection)) {
                openProxySession(oracleConnection);
            }
        }

        boolean isProxyUserNameConfigured() {
            var jdbcProperties = configuration.getJdbcProperties();
            return jdbcProperties != null &&
                   jdbcProperties.containsKey(ClassUtils.getStaticFieldValue(oracleConnectionClass, "PROXY_USER_NAME"));
        }

        private void openProxySession(Object oracleConnection) throws SQLException {
            Properties props = new Properties();
            props.putAll(configuration.getJdbcProperties());
            var proxytypeUserName = ClassUtils.getStaticFieldValue(oracleConnectionClass, "PROXYTYPE_USER_NAME");
            ClassUtils.invoke(oracleConnectionClass,
                              "openProxySession",
                              oracleConnection,
                              new Class[] { String.class, Properties.class },
                              new Object[] { proxytypeUserName, props },
                              e -> e instanceof SQLException ? (SQLException) e : null);
        }
    }
}
