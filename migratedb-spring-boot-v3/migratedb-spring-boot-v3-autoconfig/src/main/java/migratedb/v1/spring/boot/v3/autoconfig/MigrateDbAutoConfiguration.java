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
package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.MigrateDb;
import migratedb.v1.core.api.*;
import migratedb.v1.core.api.callback.Callback;
import migratedb.v1.core.api.configuration.DefaultConfiguration;
import migratedb.v1.core.api.migration.JavaMigration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Daniel Huss
 */
@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(MigrateDbProperties.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ConditionalOnClass(MigrateDb.class)
@Conditional(MigrateDbAutoConfiguration.MigrateDbDataSourceCondition.class)
@ConditionalOnProperty(prefix = "migratedb", name = "enabled", matchIfMissing = true)
public class MigrateDbAutoConfiguration {

    static final class MigrateDbDataSourceCondition extends AnyNestedCondition {
        MigrateDbDataSourceCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(DataSource.class)
        static final class DataSourceBeanCondition {
        }

        @ConditionalOnProperty(prefix = "migratedb.data-source", name = "url")
        static final class UrlCondition {
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateDb migrateDb(ResourceLoader resourceLoader,
                               ObjectProvider<DataSource> applicationDataSource,
                               @MigrateDbDataSource ObjectProvider<DataSource> migrateDbDataSource,
                               ObjectProvider<MigrateDbProperties> properties,
                               ObjectProvider<MigrateDbConfigurationCustomizer> configurationCustomizers,
                               ObjectProvider<JavaMigration> javaMigrations,
                               ObjectProvider<Callback> callbacks,
                               ObjectProvider<MigrateDbExtension> extensions,
                               ObjectProvider<ExtensionConfig> extensionConfigs) {
        var configuration = new DefaultConfiguration(resourceLoader.getClassLoader());
        var propertiesIfUnique = properties.getIfUnique();
        var dataSource = configureDataSource(configuration,
                                             propertiesIfUnique,
                                             applicationDataSource.getIfUnique(),
                                             migrateDbDataSource.getIfUnique());
        configureFromProperties(configuration, propertiesIfUnique);
        configureExtensions(resourceLoader, configuration, propertiesIfUnique, extensions, extensionConfigs);
        configureCallbacks(configuration, callbacks);
        configureJavaMigrations(configuration, javaMigrations);
        configureCustomizers(configuration, configurationCustomizers);
        configuration.setExtensionConfig(SpringIntegration.class, new SpringIntegration(dataSource));
        return new MigrateDb(configuration);
    }

    @Bean
    public MigrateDbSchemaManagementProvider migrateDbSchemaManagementProvider(ObjectProvider<MigrateDb> migrateDb) {
        return new MigrateDbSchemaManagementProvider(migrateDb);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateDbInitializer migrateDbInitializer(MigrateDb migrateDb,
                                                     MigrateDbExecution migrateDbExecution) {
        return new MigrateDbInitializer(migrateDb, migrateDbExecution);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateDbExecution migrateDbExecution() {
        return new DefaultMigrateDbExecution();
    }

    private void configureFromProperties(DefaultConfiguration configuration, @Nullable MigrateDbProperties props) {
        if (props == null) {
            return;
        }
        var mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        mapper.from(props::getBaselineDescription)
              .to(configuration::setBaselineDescription);
        mapper.from(props::getBaselineMigrationPrefix)
              .to(configuration::setBaselineMigrationPrefix);
        mapper.from(props::getBaselineOnMigrate)
              .to(configuration::setBaselineOnMigrate);
        mapper.from(props::getBaselineVersion)
              .as(Version::parse)
              .to(configuration::setBaselineVersion);
        mapper.from(props.getCherryPick())
              .as(it -> it.stream().map(MigrationPattern::new).collect(Collectors.toUnmodifiableList()))
              .to(configuration::setCherryPick);
        mapper.from(props.getConnectRetries())
              .to(configuration::setConnectRetries);
        mapper.from(props.getConnectRetriesInterval())
              .as(Duration::toSeconds)
              .as(MigrateDbAutoConfiguration::saturatedCastToInt)
              .to(configuration::setConnectRetriesInterval);
        mapper.from(props::getCreateSchemas)
              .to(configuration::setCreateSchemas);
        mapper.from(props::getDefaultSchema)
              .to(configuration::setDefaultSchema);
        mapper.from(props::getEncoding)
              .to(configuration::setEncoding);
        mapper.from(props::getFailOnMissingLocations)
              .to(configuration::setFailOnMissingLocations);
        mapper.from(props::getFailOnMissingTarget)
              .to(configuration::setFailOnMissingTarget);
        mapper.from(props::getGroup)
              .to(configuration::setGroup);
        mapper.from(props::getIgnoreFutureMigrations)
              .to(configuration::setIgnoreFutureMigrations);
        mapper.from(props::getIgnoreMigrationPatterns)
              .to(configuration::setIgnoreMigrationPatterns);
        mapper.from(props::getIgnoreMissingMigrations)
              .to(configuration::setIgnoreMissingMigrations);
        mapper.from(props::getIgnorePendingMigrations)
              .to(configuration::setIgnorePendingMigrations);
        mapper.from(props::getInitSql)
              .to(configuration::setInitSql);
        mapper.from(props::getInstalledBy)
              .to(configuration::setInstalledBy);
        mapper.from(props::getLiberateOnMigrate)
              .to(configuration::setLiberateOnMigrate);
        mapper.from(props::getLocations)
              .to(configuration::setLocationsAsStrings);
        mapper.from(props::getLockRetryCount)
              .to(configuration::setLockRetryCount);
        mapper.from(props::getMixed)
              .to(configuration::setMixed);
        mapper.from(props::getOldTable)
              .to(configuration::setOldTable);
        mapper.from(props::getOutOfOrder)
              .to(configuration::setOutOfOrder);
        mapper.from(props::getOutputQueryResults)
              .to(configuration::setOutputQueryResults);
        mapper.from(props::getPlaceholderPrefix)
              .to(configuration::setPlaceholderPrefix);
        mapper.from(props::getPlaceholderReplacement)
              .to(configuration::setPlaceholderReplacement);
        mapper.from(props::getPlaceholders)
              .to(configuration::setPlaceholders);
        mapper.from(props::getPlaceholderSuffix)
              .to(configuration::setPlaceholderSuffix);
        mapper.from(props::getRepeatableSqlMigrationPrefix)
              .to(configuration::setRepeatableSqlMigrationPrefix);
        mapper.from(props::getSchemas)
              .to(configuration::setSchemas);
        mapper.from(props::getScriptPlaceholderPrefix)
              .to(configuration::setScriptPlaceholderPrefix);
        mapper.from(props::getScriptPlaceholderSuffix)
              .to(configuration::setScriptPlaceholderSuffix);
        mapper.from(props::getSkipDefaultCallbacks)
              .to(configuration::setSkipDefaultCallbacks);
        mapper.from(props::getSkipDefaultResolvers)
              .to(configuration::setSkipDefaultResolvers);
        mapper.from(props::getSkipExecutingMigrations)
              .to(configuration::setSkipExecutingMigrations);
        mapper.from(props::getSqlMigrationPrefix)
              .to(configuration::setSqlMigrationPrefix);
        mapper.from(props::getSqlMigrationSeparator)
              .to(configuration::setSqlMigrationSeparator);
        mapper.from(props::getSqlMigrationSuffixes)
              .to(configuration::setSqlMigrationSuffixes);
        mapper.from(props::getTable)
              .to(configuration::setTable);
        mapper.from(props::getTablespace)
              .to(configuration::setTablespace);
        mapper.from(props::getTarget)
              .as(TargetVersion::parse)
              .to(configuration::setTarget);
        mapper.from(props::getValidateMigrationNaming)
              .to(configuration::setValidateMigrationNaming);
        mapper.from(props::getValidateOnMigrate)
              .to(configuration::setValidateOnMigrate);
    }

    private void configureExtensions(ResourceLoader resourceLoader,
                                     DefaultConfiguration configuration,
                                     @Nullable MigrateDbProperties props,
                                     ObjectProvider<MigrateDbExtension> extensions,
                                     ObjectProvider<ExtensionConfig> extensionConfigs) {
        extensions.forEach(configuration::useExtension);
        if (props != null && props.isUseServiceLoader()) {
            addExtensionsFromServiceLoader(resourceLoader, configuration);
        }
        if (props != null && props.getExtensionConfig() != null) {
            var extensionConfigAsProperties = new HashMap<String, String>();
            props.getExtensionConfig().forEach((key, value) -> extensionConfigAsProperties.put("migratedb." + key, value));
            configuration.configure(extensionConfigAsProperties);
        }
        extensionConfigs.forEach(it -> configuration.setExtensionConfig(it.getClass().asSubclass(ExtensionConfig.class), it));
    }

    private static void addExtensionsFromServiceLoader(ResourceLoader resourceLoader, DefaultConfiguration configuration) {
        var classLoader = resourceLoader.getClassLoader();
        ServiceLoader<MigrateDbExtension> serviceLoader;
        if (classLoader == null) {
            serviceLoader = ServiceLoader.load(MigrateDbExtension.class);
        } else {
            serviceLoader = ServiceLoader.load(MigrateDbExtension.class, classLoader);
        }
        configuration.useExtensions(serviceLoader);
    }

    private void configureCustomizers(DefaultConfiguration configuration,
                                      ObjectProvider<MigrateDbConfigurationCustomizer> configurationCustomizers) {
        configurationCustomizers.orderedStream()
                                .forEach((customizer) -> customizer.customize(configuration));
    }

    private @Nullable DataSource configureDataSource(DefaultConfiguration configuration,
                                                     @Nullable MigrateDbProperties properties,
                                                     @Nullable DataSource applicationDataSource,
                                                     @Nullable DataSource migrateDbDataSource) {
        if (configuration.getDataSource() != null) {
            throw new IllegalStateException("MigrateDB configuration already has a data source set, which is unexpected");
        }

        var specializedDataSourcesByName = describeSpecializedMigrationDataSources(properties,
                                                                                   applicationDataSource,
                                                                                   migrateDbDataSource);

        var singleSpecializedDataSource = takeSingleDataSourceOrThrow(specializedDataSourcesByName);

        if (singleSpecializedDataSource != null) {
            configuration.setDataSource(singleSpecializedDataSource);
            return singleSpecializedDataSource;
        } else if (applicationDataSource != null) {
            configuration.setDataSource(applicationDataSource);
            return applicationDataSource;
        } else {
            // No data source available at all, so unless the configuration customizer sets one, migration's going to
            // fail on first use.
            return null;
        }
    }

    private Map<String, @Nullable Supplier<DataSource>> describeSpecializedMigrationDataSources(@Nullable MigrateDbProperties properties,
                                                                                                @Nullable DataSource applicationDataSource,
                                                                                                @Nullable DataSource migrateDbDataSource) {
        var dataSourcesByName = new LinkedHashMap<String, Supplier<@Nullable DataSource>>();
        dataSourcesByName.put("@" + MigrateDbDataSource.class.getSimpleName() + " bean",
                              asDataSourceSupplier(migrateDbDataSource));

        var springPropertiesDataSource = Optional.ofNullable(properties)
                                                 .map(MigrateDbProperties::getDataSource)
                                                 .map(DataSourceProperties::initializeDataSourceBuilder)
                                                 .orElse(null);
        dataSourcesByName.put("Spring properties data source [migratedb.data-source]",
                              asDataSourceSupplier(springPropertiesDataSource));

        if (properties != null && properties.getUser() != null) {
            if (applicationDataSource == null) {
                throw new MissingApplicationDataSourceException();
            }
            var derivedDataSource = new DerivedDataSource(applicationDataSource, properties.getUser(), properties.getPassword());
            dataSourcesByName.put("Data source derived from application data source using credentials" +
                                  " [migratedb.(user,password)]",
                                  asDataSourceSupplier(derivedDataSource));
        }

        return dataSourcesByName;
    }

    private @Nullable DataSource takeSingleDataSourceOrThrow(Map<String, @Nullable Supplier<DataSource>> dataSourcesByName) {
        Supplier<DataSource> result = null;
        List<String> eligibleDataSources = new ArrayList<>();
        for (var entry : dataSourcesByName.entrySet()) {
            var name = entry.getKey();
            var dataSourceSupplier = entry.getValue();
            if (dataSourceSupplier != null) {
                eligibleDataSources.add(name);
                result = dataSourceSupplier;
            }
        }
        if (eligibleDataSources.size() > 1) {
            throw new ConflictingDataSourcesException(eligibleDataSources);
        }
        return result == null ? null : result.get();
    }

    private @Nullable Supplier<DataSource> asDataSourceSupplier(@Nullable DataSource dataSource) {
        return dataSource == null ? null : () -> dataSource;
    }

    private @Nullable Supplier<DataSource> asDataSourceSupplier(@Nullable DataSourceBuilder<?> builder) {
        if (builder == null) {
            return null;
        }
        DataSource[] dataSource = {null};
        return () -> {
            if (dataSource[0] == null) {
                dataSource[0] = builder.build();
            }
            return dataSource[0];
        };
    }

    private void configureCallbacks(DefaultConfiguration configuration, ObjectProvider<Callback> callbacks) {
        var callbacksArray = callbacks.orderedStream().toArray(Callback[]::new);
        if (callbacksArray.length > 0) {
            configuration.setCallbacks(callbacksArray);
        }
    }

    private void configureJavaMigrations(DefaultConfiguration configuration, ObjectProvider<JavaMigration> migrations) {
        var migrationsArray = migrations.orderedStream().toArray(JavaMigration[]::new);
        if (migrationsArray.length > 0) {
            configuration.setJavaMigrations(migrationsArray);
        }
    }

    private static Integer saturatedCastToInt(Long it) {
        return it > Integer.MAX_VALUE ? Integer.MAX_VALUE : it < Integer.MIN_VALUE ? Integer.MIN_VALUE : it.intValue();
    }
}
