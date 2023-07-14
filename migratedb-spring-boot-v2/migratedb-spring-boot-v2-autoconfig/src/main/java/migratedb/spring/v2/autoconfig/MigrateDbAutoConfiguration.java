/*
 * Copyright 2012-2019 the original author or authors.
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
package migratedb.spring.v2.autoconfig;

import migratedb.core.MigrateDb;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.configuration.ClassicConfiguration;
import migratedb.core.api.migration.JavaMigration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ConditionalOnClass(MigrateDb.class)
@ConditionalOnProperty(prefix = "spring.migratedb", name = "enabled", matchIfMissing = true)
@Import(DatabaseInitializationDependencyConfigurer.class)
class MigrateDbAutoConfiguration {

    @Bean
    public MigrateDb migrateDb(ResourceLoader resourceLoader,
                               ObjectProvider<DataSource> applicationDataSource,
                               @MigrateDbDataSource ObjectProvider<DataSource> migrateDbDataSource,
                               ObjectProvider<MigrateDbProperties> properties,
                               ObjectProvider<MigrateDbConfigurationCustomizer> configurationCustomizers,
                               ObjectProvider<JavaMigration> javaMigrations,
                               ObjectProvider<Callback> callbacks) {
        var configuration = new ClassicConfiguration(resourceLoader.getClassLoader());
        properties.ifAvailable(configuration::configure);
        configureDataSource(configuration, applicationDataSource.getIfUnique(), migrateDbDataSource.getIfAvailable());
        applyCustomizers(configuration, configurationCustomizers);
        configureCallbacks(configuration, callbacks);
        configureJavaMigrations(configuration, javaMigrations);
        return new MigrateDb(configuration);
    }

    private void applyCustomizers(ClassicConfiguration configuration,
                                  ObjectProvider<MigrateDbConfigurationCustomizer> configurationCustomizers) {
        configurationCustomizers.orderedStream()
                .forEach((customizer) -> customizer.customize(configuration));
    }

    private void configureDataSource(ClassicConfiguration configuration,
                                     @Nullable DataSource applicationDataSource,
                                     @Nullable DataSource migrateDbDataSource) {
        if (migrateDbDataSource != null) {
            // User intent is probably that this specially marked data source object wins over any properties-based
            // configuration and over the application's default data source.
            configuration.setDataSource(migrateDbDataSource);
        } else if (configuration.getDataSource() == null && applicationDataSource != null) {
            // User hasn't configured a data source in the properties, but the application has a default data source,
            // so we probably want to use that one.
            configuration.setDataSource(applicationDataSource);
        } else {
            // No data source available at all, so unless the configuration customizer sets one, it's going to fail on
            // first use.
        }
    }

    private void configureCallbacks(ClassicConfiguration configuration, ObjectProvider<Callback> callbacks) {
        // Note: If there are any callback beans we discard the properties-based ones provided as class names -
        // I hope that's what users would expect.
        var callbacksArray = callbacks.orderedStream().toArray(Callback[]::new);
        if (callbacksArray.length > 0) {
            configuration.setCallbacks(callbacksArray);
        }
    }

    private void configureJavaMigrations(ClassicConfiguration configuration, ObjectProvider<JavaMigration> migrations) {
        var migrationsArray = migrations.orderedStream().toArray(JavaMigration[]::new);
        configuration.setJavaMigrations(migrationsArray);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateDbInitializer migrateDbInitializer(MigrateDb migrateDb,
                                                     MigrateDbExecution migrateDbExecution) {
        return new MigrateDbInitializer(migrateDb, migrateDbExecution);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateDbExecution defaultMigrateDbExecution() {
        return migrateDb -> {
            migrateDb.repair();
            migrateDb.migrate();
        };
    }
}
