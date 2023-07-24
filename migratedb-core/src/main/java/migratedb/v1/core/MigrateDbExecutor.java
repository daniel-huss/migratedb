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
package migratedb.v1.core;

import migratedb.v1.core.api.ClassProvider;
import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.callback.Callback;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.sqlscript.SqlScript;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.migration.JavaMigration;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.internal.callback.DefaultCallbackExecutor;
import migratedb.v1.core.internal.callback.NoopCallbackExecutor;
import migratedb.v1.core.internal.callback.SqlScriptCallbackFactory;
import migratedb.v1.core.internal.configuration.ConfigurationValidator;
import migratedb.v1.core.internal.jdbc.JdbcConnectionFactoryImpl;
import migratedb.v1.core.internal.parser.ParsingContextImpl;
import migratedb.v1.core.internal.resolver.DefaultMigrationResolver;
import migratedb.v1.core.internal.resource.ResourceNameValidator;
import migratedb.v1.core.internal.resource.StringResource;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import migratedb.v1.core.internal.schemahistory.SchemaHistoryFactory;
import migratedb.v1.core.internal.util.LocationScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MigrateDbExecutor {
    private static final Log LOG = Log.getLog(MigrateDbExecutor.class);

    static final class CommandContext {
        public final MigrationResolver migrationResolver;
        public final SchemaHistory schemaHistory;
        public final Database<?> database;
        public final Schema<?, ?> defaultSchema;
        public final Schema<?, ?>[] schemas;
        public final CallbackExecutor callbackExecutor;

        CommandContext(MigrationResolver migrationResolver,
                       SchemaHistory schemaHistory,
                       Database<?> database,
                       Schema<?, ?> defaultSchema,
                       Schema<?, ?>[] schemas,
                       CallbackExecutor callbackExecutor) {
            this.migrationResolver = migrationResolver;
            this.schemaHistory = schemaHistory;
            this.database = database;
            this.defaultSchema = defaultSchema;
            this.schemas = schemas;
            this.callbackExecutor = callbackExecutor;
        }
    }

    interface Command<T> {
        T execute(CommandContext context);
    }

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
     * @return The result of the command.
     */
    public <T> T execute(Command<T> command, boolean scannerRequired) {
        var logSystem = configuration.getLogger();
        if (logSystem != null) {
            return Log.withLogSystem(logSystem, () -> doExecute(command, scannerRequired));
        } else {
            return doExecute(command, scannerRequired);
        }
    }

    private <T> T doExecute(Command<T> command, boolean scannerRequired) {
        T result;

        configurationValidator.validate(configuration);

        var resourceProviderClassProviders = createResourceAndClassProviders(scannerRequired);
        ResourceProvider resourceProvider = resourceProviderClassProviders.resourceProvider;
        ClassProvider<JavaMigration> classProvider = resourceProviderClassProviders.classProvider;

        resourceNameValidator.validateSQLMigrationNaming(resourceProvider, configuration);

        var jdbcConnectionFactory = new JdbcConnectionFactoryImpl(configuration.getDataSource(),
                configuration);

        var databaseType = jdbcConnectionFactory.getDatabaseType();
        var parsingContext = new ParsingContextImpl();
        var sqlScriptFactory = databaseType.createSqlScriptFactory(configuration, parsingContext);

        var noCallbackSqlScriptExecutorFactory = databaseType.createSqlScriptExecutorFactory(
                jdbcConnectionFactory,
                NoopCallbackExecutor.INSTANCE
        );

        jdbcConnectionFactory.setConnectionInitializer((jdbcConnectionFactory1, connection) -> {
            if (configuration.getInitSql() == null) {
                return;
            }
            StringResource resource = new StringResource("", configuration.getInitSql());
            SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, true, resourceProvider);
            boolean outputQueryResults = configuration.isOutputQueryResults();
            noCallbackSqlScriptExecutorFactory.createSqlScriptExecutor(connection, outputQueryResults)
                    .execute(sqlScript);
        });

        try (var database = databaseType.createDatabase(configuration, !dbConnectionInfoPrinted, jdbcConnectionFactory)) {
            dbConnectionInfoPrinted = true;
            LOG.debug("DDL transactions supported: " + database.supportsDdlTransactions());

            var schemas = SchemaHistoryFactory.scanSchemas(configuration, database);

            parsingContext.populate(database, configuration);

            database.ensureSupported();

            var callbackExecutor = new DefaultCallbackExecutor(configuration,
                    database,
                    schemas.defaultSchema,
                    prepareCallbacks(
                            database,
                            resourceProvider,
                            jdbcConnectionFactory,
                            sqlScriptFactory,
                            schemas.defaultSchema,
                            parsingContext));

            var sqlScriptExecutorFactory = databaseType.createSqlScriptExecutorFactory(
                    jdbcConnectionFactory,
                    callbackExecutor);

            var schemaHistory = SchemaHistoryFactory.getSchemaHistory(
                    configuration,
                    noCallbackSqlScriptExecutorFactory,
                    sqlScriptFactory,
                    database,
                    schemas.defaultSchema);

            var commandContext = new CommandContext(
                    createMigrationResolver(resourceProvider,
                            classProvider,
                            sqlScriptExecutorFactory,
                            sqlScriptFactory,
                            parsingContext),
                    schemaHistory,
                    database,
                    schemas.defaultSchema,
                    schemas.all.toArray(new Schema[0]),
                    callbackExecutor
            );

            result = command.execute(commandContext);
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

    private ResourceAndClassProviders createResourceAndClassProviders(boolean scannerRequired) {
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
                        configuration.getLocations(),
                        configuration.getClassLoader(),
                        configuration.isFailOnMissingLocations()
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

    private List<Callback> prepareCallbacks(Database<?> database,
                                            ResourceProvider resourceProvider,
                                            JdbcConnectionFactory jdbcConnectionFactory,
                                            SqlScriptFactory sqlScriptFactory,
                                            Schema<?, ?> schema,
                                            ParsingContext parsingContext) {
        List<Callback> effectiveCallbacks = new ArrayList<>(configuration.getCallbacks());

        if (!configuration.isSkipDefaultCallbacks()) {
            // The no-op callback executor here is intentional because we're just interested in
            // SqlScriptCallbackFactory.getCallbacks() and somehow have to satisfy the ctor dependencies.
            var sqlScriptExecutorFactory = jdbcConnectionFactory.getDatabaseType()
                    .createSqlScriptExecutorFactory(
                            jdbcConnectionFactory,
                            NoopCallbackExecutor.INSTANCE);

            effectiveCallbacks.addAll(new SqlScriptCallbackFactory(resourceProvider,
                    sqlScriptExecutorFactory,
                    sqlScriptFactory,
                    configuration).getCallbacks());
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
     * @return A new, fully configured, MigrationResolver instance.
     */
    private MigrationResolver createMigrationResolver(ResourceProvider resourceProvider,
                                                      ClassProvider<JavaMigration> classProvider,
                                                      SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                                      SqlScriptFactory sqlScriptFactory,
                                                      ParsingContext parsingContext) {
        return new DefaultMigrationResolver(resourceProvider,
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
