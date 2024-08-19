/*
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

package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.api.ExtensionConfig;
import migratedb.v1.core.api.configuration.Configuration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Extension to the MigrateDB {@link Configuration} that remembers the Spring-managed
 * {@link DataSource} instance used by that configuration. Is needed by the {@link SchemaManagementProvider}
 * autoconfiguration bean for detecting the {@link SchemaManagement} of a data source.
 */
public final class SpringIntegration implements ExtensionConfig {
    private final @Nullable DataSource managedDataSource;

    public SpringIntegration(@Nullable DataSource managedDataSource) {
        this.managedDataSource = managedDataSource;
    }

    public @Nullable DataSource getManagedDataSource() {
        return managedDataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        var other = (SpringIntegration) o;
        return Objects.equals(managedDataSource, other.managedDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(managedDataSource);
    }
}
