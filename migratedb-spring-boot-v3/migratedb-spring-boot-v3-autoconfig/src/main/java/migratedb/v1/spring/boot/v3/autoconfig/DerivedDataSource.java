/*
 * Copyright 2023 The MigrateDB contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package migratedb.v1.spring.boot.v3.autoconfig;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A delegating data source that uses the given credentials for {@link #getConnection()}.
 */
final class DerivedDataSource implements DataSource {
    private final DataSource delegate;
    private final String user;
    private final @Nullable String password;

    DerivedDataSource(DataSource delegate, String user, @Nullable String password) {
        this.delegate = Objects.requireNonNull(delegate);
        this.user = Objects.requireNonNull(user);
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection(user, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return delegate.createConnectionBuilder();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        return delegate.createShardingKeyBuilder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface == DataSource.class) {
            return (T) delegate;
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface == DataSource.class || delegate.isWrapperFor(iface);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (DerivedDataSource) o;
        return Objects.equals(delegate, other.delegate) &&
               Objects.equals(user, other.user) &&
               Objects.equals(password, other.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, user, password);
    }

    @Override
    public String toString() {
        return "DerivedDataSource{" +
               "delegate=" + delegate +
               '}';
    }
}
