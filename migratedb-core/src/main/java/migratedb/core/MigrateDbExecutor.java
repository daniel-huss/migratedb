/*
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
package migratedb.core;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import migratedb.core.api.ClassProvider;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.logging.Log;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.callback.CallbackExecutor;
import migratedb.core.internal.callback.DefaultCallbackExecutor;
import migratedb.core.internal.callback.NoopCallbackExecutor;
import migratedb.core.internal.callback.SqlScriptCallbackFactory;
import migratedb.core.internal.configuration.ConfigurationValidator;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.resolver.CompositeMigrationResolver;
import migratedb.core.internal.resource.ResourceNameValidator;
import migratedb.core.internal.resource.StringResource;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.schemahistory.SchemaHistoryFactory;
import migratedb.core.internal.sqlscript.SqlScript;
import migratedb.core.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.sqlscript.SqlScriptFactory;
import migratedb.core.internal.strategy.RetryStrategy;
import migratedb.core.internal.util.LocationScanner;

final class MigrateDbExecutor {
    private static final Log LOG = Log.getLog(MigrateDbExecutor.class);

    /**
     * Designed so we can fail fast if the configuration is invalid
     */
    private final ConfigurationValidator configurationValidator;
    /**
     * Designed so we can fail fast if the SQL file resources are invalid
     */
    private final ResourceNameValidator resourceNameValidator;
    /**
     * Whether the database connection info has already been printed in the logs
     */
    private boolean dbConnectionInfoPrinted;
    private final Configuration configuration;

    MigrateDbExecutor(Configuration configuration) {
        this.configurationValidator = new ConfigurationValidator();
        this.resourceNameValidator = new ResourceNameValidator();
        this.configuration = configuration;
    }

    /**
     * Executes this command with proper resource handling and cleanup.
     *
     * @param command The command to execute.
     * @param <T>     The type of the result.
     *
     * @return The result of the command.
     */
    public <T> T execute(Command<T> command, boolean scannerRequired) {
        T result;

        configurationValidator.validate(configuration);

        StatementInterceptor statementInterceptor = null;

        var resourceProviderClassProviders = createResourceAndClassProviders(scannerRequired);
        ResourceProvider resourceProvider = resourceProviderClassProviders.resourceProvider;
        ClassProvider<JavaMigration> classProvider = resourceProviderClassProviders.classProvider;

        if (configuration.isValidateMigrationNaming()) {
            resourceNameValidator.validateSQLMigrationNaming(resourceProvider, configuration);
        }

        JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory(
            configuration.getDataSource(),
            configuration,
            statementInterceptor
        );

        DatabaseType databaseType = jdbcConnectionFactory.getDatabaseType();
        ParsingContext parsingContext = new ParsingContext();
        SqlScriptFactory sqlScriptFactory = databaseType.createSqlScriptFactory(configuration, parsingContext);
        RetryStrategy.setNumberOfRetries(configuration.getLockRetryCount());

        SqlScriptExecutorFactory noCallbackSqlScriptExecutorFactory = databaseType.createSqlScriptExecutorFactory(
            jdbcConnectionFactory,
            NoopCallbackExecutor.INSTANCE,
            null
        );

        jdbcConnectionFactory.setConnectionInitializer(new JdbcConnectionFactory.ConnectionInitializer() {
            @Override
            public void initialize(JdbcConnectionFactory jdbcConnectionFactory, Connection connection) {
                if (configuration.getInitSql() == null) {
                    return;
                }
                StringResource resource = new StringResource("", configuration.getInitSql());

                SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, true, resourceProvider);

                boolean outputQueryResults = false;

                noCallbackSqlScriptExecutorFactory.createSqlScriptExecutor(connection, false, false, outputQueryResults)
                                                  .execute(sqlScript);
            }
        });

        try (var database = databaseType.createDatabase(configuration,
                                                        !dbConnectionInfoPrinted,
                                                        jdbcConnectionFactory,
                                                        statementInterceptor)) {
            dbConnectionInfoPrinted = true;
            LOG.debug("DDL Transactions Supported: " + database.supportsDdlTransactions());

            var schemas = SchemaHistoryFactory.scanSchemas(configuration, database);

            parsingContext.populate(database, configuration);

            database.ensureSupported();

            DefaultCallbackExecutor callbackExecutor =
                new DefaultCallbackExecutor(configuration, database, schemas.defaultSchema,
                                            prepareCallbacks(
                                                database,
                                                resourceProvider,
                                                jdbcConnectionFactory,
                                                sqlScriptFactory,
                                                statementInterceptor,
                                                schemas.defaultSchema,
                                                parsingContext
                                            ));

            SqlScriptExecutorFactory sqlScriptExecutorFactory =
                databaseType.createSqlScriptExecutorFactory(jdbcConnectionFactory,
                                                            callbackExecutor,
                                                            statementInterceptor);

            SchemaHistory schemaHistory = SchemaHistoryFactory.getSchemaHistory(
                configuration,
                noCallbackSqlScriptExecutorFactory,
                sqlScriptFactory,
                database,
                schemas.defaultSchema,
                statementInterceptor);

            result = command.execute(
                createMigrationResolver(resourceProvider,
                                        classProvider,
                                        sqlScriptExecutorFactory,
                                        sqlScriptFactory,
                                        parsingContext),
                schemaHistory,
                database,
                schemas.all.toArray(new Schema[0]),
                callbackExecutor,
                statementInterceptor);
        } finally {
            showMemoryUsage();
        }
        return result;
    }

    private static final class ResourceAndClassProviders {
        public final ResourceProvider resourceProvider;
        public final ClassProvider<JavaMigration> classProvider;

        private ResourceAndClassProviders(ResourceProvider resourceProvider,
                                          ClassProvider<JavaMigration> classProvider) {
            this.resourceProvider = resourceProvider;
            this.classProvider = classProvider;
        }
    }

    private ResourceAndClassProviders createResourceAndClassProviders(
        boolean scannerRequired) {
        ResourceProvider resourceProvider;
        ClassProvider<JavaMigration> classProvider;
        if (!scannerRequired && configuration.isSkipDefaultResolvers() && configuration.isSkipDefaultCallbacks()) {
            resourceProvider = ResourceProvider.noResources();
            classProvider = ClassProvider.noClasses();
        } else {
            if (configuration.getResourceProvider() != null && configuration.getJavaMigrationClassProvider() != null) {
                resourceProvider = configuration.getResourceProvider();
                classProvider = configuration.getJavaMigrationClassProvider();
            } else {
                LocationScanner<JavaMigration> scanner = new LocationScanner<>(
                    JavaMigration.class,
                    Arrays.asList(configuration.getLocations()),
                    configuration.getClassLoader(),
                    configuration.getFailOnMissingLocations()
                );
                // set the defaults
                resourceProvider = scanner;
                classProvider = scanner;
                if (configuration.getResourceProvider() != null) {
                    resourceProvider = configuration.getResourceProvider();
                }
                if (configuration.getJavaMigrationClassProvider() != null) {
                    classProvider = configuration.getJavaMigrationClassProvider();
                }
            }
        }

        return new ResourceAndClassProviders(resourceProvider, classProvider);
    }

    private List<Callback> prepareCallbacks(Database database, ResourceProvider resourceProvider,
                                            JdbcConnectionFactory jdbcConnectionFactory,
                                            SqlScriptFactory sqlScriptFactory,
                                            StatementInterceptor statementInterceptor,
                                            Schema schema,
                                            ParsingContext parsingContext) {
        List<Callback> effectiveCallbacks = new ArrayList<>();
        CallbackExecutor callbackExecutor = NoopCallbackExecutor.INSTANCE;

        effectiveCallbacks.addAll(Arrays.asList(configuration.getCallbacks()));

        if (!configuration.isSkipDefaultCallbacks()) {
            SqlScriptExecutorFactory sqlScriptExecutorFactory =
                jdbcConnectionFactory.getDatabaseType().createSqlScriptExecutorFactory(jdbcConnectionFactory,
                                                                                       callbackExecutor,
                                                                                       statementInterceptor
                );

            effectiveCallbacks.addAll(
                new SqlScriptCallbackFactory(
                    resourceProvider,
                    sqlScriptExecutorFactory,
                    sqlScriptFactory,
                    configuration
                ).getCallbacks());
        }

        return effectiveCallbacks;
    }

    /**
     * Creates the MigrationResolver.
     *
     * @param resourceProvider The resource provider.
     * @param classProvider    The class provider.
     * @param sqlScriptFactory The SQL statement builder factory.
     * @param parsingContext   The parsing context.
     *
     * @return A new, fully configured, MigrationResolver instance.
     */
    private MigrationResolver createMigrationResolver(ResourceProvider resourceProvider,
                                                      ClassProvider<JavaMigration> classProvider,
                                                      SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                                      SqlScriptFactory sqlScriptFactory,
                                                      ParsingContext parsingContext) {
        return new CompositeMigrationResolver(resourceProvider,
                                              classProvider,
                                              configuration,
                                              sqlScriptExecutorFactory,
                                              sqlScriptFactory,
                                              parsingContext,
                                              configuration.getResolvers());
    }

    private void showMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long used = total - free;

        long totalMB = total / (1024 * 1024);
        long usedMB = used / (1024 * 1024);
        LOG.debug("Memory usage: " + usedMB + " of " + totalMB + "M");
    }
}
