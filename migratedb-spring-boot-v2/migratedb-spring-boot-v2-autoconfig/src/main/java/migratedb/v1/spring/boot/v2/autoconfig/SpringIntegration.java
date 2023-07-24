package migratedb.v1.spring.boot.v2.autoconfig;

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
