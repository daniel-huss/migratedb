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
package migratedb.core;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import migratedb.core.api.ClassProvider;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.ClassicConfiguration;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.api.exception.MigrateDbValidateException;
import migratedb.core.api.logging.Log;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.output.BaselineResult;
import migratedb.core.api.output.CleanResult;
import migratedb.core.api.output.MigrateResult;
import migratedb.core.api.output.RepairResult;
import migratedb.core.api.output.UndoResult;
import migratedb.core.api.output.ValidateResult;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.callback.CallbackExecutor;
import migratedb.core.internal.callback.DefaultCallbackExecutor;
import migratedb.core.internal.callback.NoopCallbackExecutor;
import migratedb.core.internal.callback.SqlScriptCallbackFactory;
import migratedb.core.internal.clazz.NoopClassProvider;
import migratedb.core.internal.command.DbBaseline;
import migratedb.core.internal.command.DbClean;
import migratedb.core.internal.command.DbInfo;
import migratedb.core.internal.command.DbMigrate;
import migratedb.core.internal.command.DbRepair;
import migratedb.core.internal.command.DbSchemas;
import migratedb.core.internal.command.DbValidate;
import migratedb.core.internal.configuration.ConfigurationValidator;
import migratedb.core.internal.database.DatabaseType;
import migratedb.core.internal.database.base.Database;
import migratedb.core.internal.database.base.Schema;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.resolver.CompositeMigrationResolver;
import migratedb.core.internal.resource.NoopResourceProvider;
import migratedb.core.internal.resource.ResourceNameValidator;
import migratedb.core.internal.resource.StringResource;
import migratedb.core.internal.scanner.LocationScannerCache;
import migratedb.core.internal.scanner.ResourceNameCache;
import migratedb.core.internal.scanner.Scanner;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.schemahistory.SchemaHistoryFactory;
import migratedb.core.internal.sqlscript.SqlScript;
import migratedb.core.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.sqlscript.SqlScriptFactory;
import migratedb.core.internal.strategy.RetryStrategy;
import migratedb.core.internal.util.Development;
import migratedb.core.internal.util.IOUtils;
import migratedb.core.internal.util.MigrateDbWebsiteLinks;
import migratedb.core.internal.util.Pair;
import migratedb.core.internal.util.StringUtils;

/**
 * This is the centre point of MigrateDB, and for most users, the only class they will ever have to deal with.
 * <p>
 * It is THE public API from which all important MigrateDB functions such as clean, validate and migrate can be called.
 * </p>
 * <p>To get started all you need to do is create a configured MigrateDb object and then invoke its principal
 * methods.</p>
 * <pre>
 * MigrateDb migratedb = MigrateDb.configure().dataSource(url, user, password).load();
 * migratedb.migrate();
 * </pre>
 * Note that a configured MigrateDb object is unmodifiable.
 */
public class MigrateDb {
    private static final Log LOG = Log.getLog(MigrateDb.class);

    private final ClassicConfiguration configuration;

    /**
     * Whether the database connection info has already been printed in the logs.
     */
    private boolean dbConnectionInfoPrinted;

    /**
     * Designed so we can fail fast if the configuration is invalid.
     */
    private final ConfigurationValidator configurationValidator = new ConfigurationValidator();

    /**
     * Designed so we can fail fast if the SQL file resources are invalid.
     */
    private final ResourceNameValidator resourceNameValidator = new ResourceNameValidator();

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being
     * loaded into a new MigrateDb instance using the load() method.
     * <p>In its simplest form, this is how you configure MigrateDb with all defaults to get started:</p>
     * <pre>MigrateDb migratedb = MigrateDb.configure().dataSource(url, user, password).load();</pre>
     * <p>After that you have a fully-configured MigrateDb instance at your disposal which can be used to invoke
     * MigrateDb functionality such as migrate() or clean().</p>
     *
     * @return A new configuration from which MigrateDb can be loaded.
     */
    public static FluentConfiguration configure() {
        return new FluentConfiguration();
    }

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being
     * loaded into a new MigrateDb instance using the load() method.
     * <p>In its simplest form, this is how you configure MigrateDb with all defaults to get started:</p>
     * <pre>MigrateDb migratedb = MigrateDb.configure().dataSource(url, user, password).load();</pre>
     * <p>After that you have a fully-configured MigrateDb instance at your disposal which can be used to invoke
     * MigrateDb functionality such as migrate() or clean().</p>
     *
     * @param classLoader The class loader to use when loading classes and resources.
     *
     * @return A new configuration from which MigrateDb can be loaded.
     */
    public static FluentConfiguration configure(ClassLoader classLoader) {
        return new FluentConfiguration(classLoader);
    }

    /**
     * Creates a new instance of MigrateDb with this configuration. In general the MigrateDb.configure() factory method
     * should be preferred over this constructor, unless you need to create or reuse separate Configuration objects.
     *
     * @param configuration The configuration to use.
     */
    public MigrateDb(Configuration configuration) {
        this.configuration = new ClassicConfiguration(configuration);

        // Load callbacks from default package
        this.configuration.loadCallbackLocation("db/callback", false);
    }

    /**
     * @return The configuration that MigrateDb is using.
     */
    public Configuration getConfiguration() {
        return new ClassicConfiguration(configuration);
    }

    /**
     * Used to cache resource names for classpath scanning between commands
     */
    private final ResourceNameCache resourceNameCache = new ResourceNameCache();

    /**
     * Used to cache LocationScanners between commands
     */
    private final LocationScannerCache locationScannerCache = new LocationScannerCache();

    /**
     * <p>Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     * <img src="https://no-website-yet.org/assets/balsamiq/command-migrate.png" alt="migrate">
     *
     * @return An object summarising the successfully applied migrations.
     *
     * @throws MigrateDbException when the migration failed.
     */
    public MigrateResult migrate() throws MigrateDbException {
        return execute(new Command<MigrateResult>() {
            public MigrateResult execute(MigrationResolver migrationResolver,
                                         SchemaHistory schemaHistory, Database database, Schema[] schemas,
                                         CallbackExecutor callbackExecutor,
                                         StatementInterceptor statementInterceptor) {
                if (configuration.isValidateOnMigrate()) {
                    ValidateResult validateResult = doValidate(database,
                                                               migrationResolver,
                                                               schemaHistory,
                                                               schemas,
                                                               callbackExecutor,
                                                               true);
                    if (!validateResult.validationSuccessful && !configuration.isCleanOnValidationError()) {
                        throw new MigrateDbValidateException(validateResult.errorDetails,
                                                             validateResult.getAllErrorMessages());
                    }
                }

                if (!schemaHistory.exists()) {
                    List<Schema> nonEmptySchemas = new ArrayList<>();
                    for (Schema schema : schemas) {
                        if (schema.exists() && !schema.empty()) {
                            nonEmptySchemas.add(schema);
                        }
                    }

                    if (!nonEmptySchemas.isEmpty()

                    ) {
                        if (configuration.isBaselineOnMigrate()) {
                            doBaseline(schemaHistory, callbackExecutor, database);
                        } else {
                            // Second check for MySQL which is sometimes flaky otherwise
                            if (!schemaHistory.exists()) {
                                throw new MigrateDbException("Found non-empty schema(s) "
                                                             + StringUtils.collectionToCommaDelimitedString(
                                    nonEmptySchemas)
                                                             + " but no schema history table. Use baseline()"
                                                             +
                                                             " or set baselineOnMigrate to true to initialize the " +
                                                             "schema history table.");
                            }
                        }
                    } else {
                        if (configuration.getCreateSchemas()) {
                            new DbSchemas(database, schemas, schemaHistory, callbackExecutor).create(false);
                        } else if (!schemas[0].exists()) {
                            LOG.warn("The configuration option 'createSchemas' is false.\n" +
                                     "However, the schema history table still needs a schema to reside in.\n" +
                                     "You must manually create a schema for the schema history table to reside in.\n" +
                                     "See " +MigrateDbWebsiteLinks.CREATE_SCHEMAS);
                        }

                        schemaHistory.create(false);
                    }
                }

                MigrateResult result = new DbMigrate(database,
                                                     schemaHistory,
                                                     schemas[0],
                                                     migrationResolver,
                                                     configuration,
                                                     callbackExecutor).migrate();

                callbackExecutor.onOperationFinishEvent(Event.AFTER_MIGRATE_OPERATION_FINISH, result);

                return result;
            }
        }, true);
    }

    private BaselineResult doBaseline(SchemaHistory schemaHistory, CallbackExecutor callbackExecutor,
                                      Database database) {
        return new DbBaseline(schemaHistory, configuration.getBaselineVersion(), configuration.getBaselineDescription(),
                              callbackExecutor, database).baseline();
    }

    /**
     * <p>Undoes the most recently applied versioned migration. If target is specified, MigrateDb will attempt to undo
     * versioned migrations in the order they were applied until it hits one with a version below the target. If there
     * is no versioned migration to undo, calling undo has no effect.</p>
     *
     * <img src="https://no-website-yet.org/assets/balsamiq/command-undo.png" alt="undo">
     *
     * @return An object summarising the successfully undone migrations.
     *
     * @throws MigrateDbException when the undo failed.
     */
    public UndoResult undo() throws MigrateDbException {
        return Development.TODO("Implement Undo");
    }

    /**
     * <p>Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.</p>
     * <p>Validation fails if</p>
     * <ul>
     * <li>differences in migration names, types or checksums are found</li>
     * <li>versions have been applied that aren't resolved locally anymore</li>
     * <li>versions have been resolved that haven't been applied yet</li>
     * </ul>
     *
     * <img src="https://no-website-yet.org/assets/balsamiq/command-validate.png" alt="validate">
     *
     * @throws MigrateDbException when the validation failed.
     */
    public void validate() throws MigrateDbException {
        execute(new Command<Void>() {
            public Void execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory, Database database,
                                Schema[] schemas, CallbackExecutor callbackExecutor,
                                StatementInterceptor statementInterceptor) {
                ValidateResult validateResult = doValidate(database,
                                                           migrationResolver,
                                                           schemaHistory,
                                                           schemas,
                                                           callbackExecutor,
                                                           configuration.isIgnorePendingMigrations());

                callbackExecutor.onOperationFinishEvent(Event.AFTER_VALIDATE_OPERATION_FINISH, validateResult);

                if (!validateResult.validationSuccessful && !configuration.isCleanOnValidationError()) {
                    throw new MigrateDbValidateException(validateResult.errorDetails,
                                                         validateResult.getAllErrorMessages());
                }

                return null;
            }
        }, true);
    }

    /**
     * <p>Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.</p>
     * <p>Validation fails if</p>
     * <ul>
     * <li>differences in migration names, types or checksums are found</li>
     * <li>versions have been applied that aren't resolved locally anymore</li>
     * <li>versions have been resolved that haven't been applied yet</li>
     * </ul>
     *
     * <img src="https://no-website-yet.org/assets/balsamiq/command-validate.png" alt="validate">
     *
     * @throws MigrateDbException when the validation failed.
     * @returns An object summarising the validation results
     */
    public ValidateResult validateWithResult() throws MigrateDbException {
        return execute(new Command<ValidateResult>() {
            public ValidateResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                          Database database,
                                          Schema[] schemas, CallbackExecutor callbackExecutor,
                                          StatementInterceptor statementInterceptor) {
                ValidateResult validateResult = doValidate(database,
                                                           migrationResolver,
                                                           schemaHistory,
                                                           schemas,
                                                           callbackExecutor,
                                                           configuration.isIgnorePendingMigrations());

                callbackExecutor.onOperationFinishEvent(Event.AFTER_VALIDATE_OPERATION_FINISH, validateResult);

                return validateResult;
            }
        }, true);
    }

    /**
     * Performs the actual validation. All set up must have taken place beforehand.
     *
     * @param database          The database-specific support.
     * @param migrationResolver The migration resolver;
     * @param schemaHistory     The schema history table.
     * @param schemas           The schemas managed by MigrateDb.
     * @param callbackExecutor  The callback executor.
     * @param ignorePending     Whether to ignore pending migrations.
     */
    private ValidateResult doValidate(Database database, MigrationResolver migrationResolver,
                                      SchemaHistory schemaHistory,
                                      Schema[] schemas, CallbackExecutor callbackExecutor, boolean ignorePending) {
        ValidateResult validateResult = new DbValidate(database, schemaHistory, schemas[0], migrationResolver,
                                                       configuration, ignorePending, callbackExecutor).validate();

        if (!validateResult.validationSuccessful && configuration.isCleanOnValidationError()) {
            doClean(database, schemaHistory, schemas, callbackExecutor);
        }

        return validateResult;
    }

    private CleanResult doClean(Database database, SchemaHistory schemaHistory, Schema[] schemas,
                                CallbackExecutor callbackExecutor) {
        return new DbClean(database, schemaHistory, schemas, callbackExecutor, configuration.isCleanDisabled()).clean();
    }

    /**
     * <p>Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
     * The schemas are cleaned in the order specified by the {@code schemas} property.</p>
     * <img src="https://no-website-yet.org/assets/balsamiq/command-clean.png" alt="clean">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the clean fails.
     */
    public CleanResult clean() {
        return execute(new Command<CleanResult>() {
            public CleanResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                       Database database,
                                       Schema[] schemas, CallbackExecutor callbackExecutor,
                                       StatementInterceptor statementInterceptor) {
                CleanResult cleanResult = doClean(database, schemaHistory, schemas, callbackExecutor);

                callbackExecutor.onOperationFinishEvent(Event.AFTER_CLEAN_OPERATION_FINISH, cleanResult);

                return cleanResult;
            }
        }, false);
    }

    /**
     * <p>Retrieves the complete information about all the migrations including applied, pending and current migrations
     * with details and status.</p>
     * <img src="https://no-website-yet.org/assets/balsamiq/command-info.png" alt="info">
     *
     * @return All migrations sorted by version, oldest first.
     *
     * @throws MigrateDbException when the info retrieval failed.
     */
    public MigrationInfoService info() {
        return execute(new Command<MigrationInfoService>() {
            public MigrationInfoService execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                                Database database, Schema[] schemas, CallbackExecutor callbackExecutor,
                                                StatementInterceptor statementInterceptor) {
                MigrationInfoService migrationInfoService = new DbInfo(migrationResolver,
                                                                       schemaHistory,
                                                                       configuration,
                                                                       database,
                                                                       callbackExecutor,
                                                                       schemas).info();

                callbackExecutor.onOperationFinishEvent(Event.AFTER_INFO_OPERATION_FINISH,
                                                        migrationInfoService.getInfoResult());

                return migrationInfoService;
            }
        }, true);
    }

    /**
     * <p>Baselines an existing database, excluding all migrations up to and including baselineVersion.</p>
     *
     * <img src="https://no-website-yet.org/assets/balsamiq/command-baseline.png" alt="baseline">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the schema baselining failed.
     */
    public BaselineResult baseline() throws MigrateDbException {
        return execute(new Command<BaselineResult>() {
            public BaselineResult execute(MigrationResolver migrationResolver,
                                          SchemaHistory schemaHistory, Database database, Schema[] schemas,
                                          CallbackExecutor callbackExecutor,
                                          StatementInterceptor statementInterceptor) {
                if (configuration.getCreateSchemas()) {
                    new DbSchemas(database, schemas, schemaHistory, callbackExecutor).create(true);
                } else {
                    LOG.warn("The configuration option 'createSchemas' is false.\n" +
                             "Even though MigrateDb is configured not to create any schemas, the schema history table" +
                             " still needs a schema to reside in.\n" +
                             "You must manually create a schema for the schema history table to reside in.\n" +
                             "See " + MigrateDbWebsiteLinks.CREATE_SCHEMAS);
                }

                BaselineResult baselineResult = doBaseline(schemaHistory, callbackExecutor, database);

                callbackExecutor.onOperationFinishEvent(Event.AFTER_BASELINE_OPERATION_FINISH, baselineResult);

                return baselineResult;
            }
        }, false);
    }

    /**
     * Repairs the MigrateDb schema history table. This will perform the following actions:
     * <ul>
     * <li>Remove any failed migrations on databases without DDL transactions (User objects left behind must still be
     * cleaned up manually)</li>
     * <li>Realign the checksums, descriptions and types of the applied migrations with the ones of the available
     * migrations</li>
     * </ul>
     * <img src="https://no-website-yet.org/assets/balsamiq/command-repair.png" alt="repair">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the schema history table repair failed.
     */
    public RepairResult repair() throws MigrateDbException {
        return execute(new Command<RepairResult>() {
            public RepairResult execute(MigrationResolver migrationResolver,
                                        SchemaHistory schemaHistory, Database database, Schema[] schemas,
                                        CallbackExecutor callbackExecutor,
                                        StatementInterceptor statementInterceptor) {
                RepairResult repairResult = new DbRepair(database,
                                                         migrationResolver,
                                                         schemaHistory,
                                                         callbackExecutor,
                                                         configuration).repair();

                callbackExecutor.onOperationFinishEvent(Event.AFTER_REPAIR_OPERATION_FINISH, repairResult);

                return repairResult;
            }
        }, true);
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

    /**
     * Executes this command with proper resource handling and cleanup.
     *
     * @param command The command to execute.
     * @param <T>     The type of the result.
     *
     * @return The result of the command.
     */
    /*private -> testing*/ <T> T execute(Command<T> command, boolean scannerRequired) {
        T result;

        configurationValidator.validate(configuration);

        StatementInterceptor statementInterceptor = null;

        Pair<ResourceProvider, ClassProvider<JavaMigration>> resourceProviderClassProviderPair =
            createResourceAndClassProviders(
                scannerRequired);
        ResourceProvider resourceProvider = resourceProviderClassProviderPair.getLeft();
        ClassProvider<JavaMigration> classProvider = resourceProviderClassProviderPair.getRight();

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
                StringResource resource = new StringResource(configuration.getInitSql());

                SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, true, resourceProvider);

                boolean outputQueryResults = false;

                noCallbackSqlScriptExecutorFactory.createSqlScriptExecutor(connection, false, false, outputQueryResults)
                                                  .execute(sqlScript);
            }
        });

        Database database = null;
        try {
            database = databaseType.createDatabase(configuration,
                                                   !dbConnectionInfoPrinted,
                                                   jdbcConnectionFactory,
                                                   statementInterceptor);
            dbConnectionInfoPrinted = true;
            LOG.debug("DDL Transactions Supported: " + database.supportsDdlTransactions());

            Pair<Schema, List<Schema>> schemas = SchemaHistoryFactory.prepareSchemas(configuration, database);
            Schema defaultSchema = schemas.getLeft();

            parsingContext.populate(database, configuration);

            database.ensureSupported();

            DefaultCallbackExecutor callbackExecutor =
                new DefaultCallbackExecutor(configuration, database, defaultSchema,
                                            prepareCallbacks(
                                                database,
                                                resourceProvider,
                                                jdbcConnectionFactory,
                                                sqlScriptFactory,
                                                statementInterceptor,
                                                defaultSchema,
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
                defaultSchema,
                statementInterceptor);

            result = command.execute(
                createMigrationResolver(resourceProvider,
                                        classProvider,
                                        sqlScriptExecutorFactory,
                                        sqlScriptFactory,
                                        parsingContext),
                schemaHistory,
                database,
                schemas.getRight().toArray(new Schema[0]),
                callbackExecutor,
                statementInterceptor);
        } finally {
            IOUtils.close(database);

            showMemoryUsage();
        }
        return result;
    }

    private Pair<ResourceProvider, ClassProvider<JavaMigration>> createResourceAndClassProviders(
        boolean scannerRequired) {
        ResourceProvider resourceProvider;
        ClassProvider<JavaMigration> classProvider;
        if (!scannerRequired && configuration.isSkipDefaultResolvers() && configuration.isSkipDefaultCallbacks()) {
            resourceProvider = NoopResourceProvider.INSTANCE;
            //noinspection unchecked
            classProvider = NoopClassProvider.INSTANCE;
        } else {
            if (configuration.getResourceProvider() != null && configuration.getJavaMigrationClassProvider() != null) {
                // don't create the scanner at all in this case
                resourceProvider = configuration.getResourceProvider();
                classProvider = configuration.getJavaMigrationClassProvider();
            } else {
                boolean stream = false;

                Scanner<JavaMigration> scanner = new Scanner<>(
                    JavaMigration.class,
                    Arrays.asList(configuration.getLocations()),
                    configuration.getClassLoader(),
                    configuration.getEncoding(),
                    configuration.getDetectEncoding(),
                    stream,
                    resourceNameCache,
                    locationScannerCache,
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

        return Pair.of(resourceProvider, classProvider);
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
     * A MigrateDb command that can be executed.
     *
     * @param <T> The result type of the command.
     */
    /*private -> testing*/ interface Command<T> {
        /**
         * Execute the operation.
         *
         * @param migrationResolver The migration resolver to use.
         * @param schemaHistory     The schema history table.
         * @param database          The database-specific support for these connections.
         * @param schemas           The schemas managed by MigrateDb.
         * @param callbackExecutor  The callback executor.
         *
         * @return The result of the operation.
         */
        T execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                  Database database, Schema[] schemas, CallbackExecutor callbackExecutor,
                  StatementInterceptor statementInterceptor
        );
    }
}
