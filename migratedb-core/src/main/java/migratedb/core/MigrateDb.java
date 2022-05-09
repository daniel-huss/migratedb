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

import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.ClassicConfiguration;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.api.exception.MigrateDbValidateException;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.BaselineResult;
import migratedb.core.api.output.CleanResult;
import migratedb.core.api.output.MigrateResult;
import migratedb.core.api.output.RepairResult;
import migratedb.core.api.output.ValidateResult;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.command.DbBaseline;
import migratedb.core.internal.command.DbClean;
import migratedb.core.internal.command.DbInfo;
import migratedb.core.internal.command.DbMigrate;
import migratedb.core.internal.command.DbRepair;
import migratedb.core.internal.command.DbSchemas;
import migratedb.core.internal.command.DbValidate;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.StringUtils;
import migratedb.core.internal.util.WebsiteLinks;

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
    private final MigrateDbExecutor migratedbExecutor;

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
        this.migratedbExecutor = new MigrateDbExecutor(configuration);
    }

    /**
     * @return The configuration that MigrateDb is using.
     */
    public Configuration getConfiguration() {
        return new ClassicConfiguration(configuration);
    }

    /**
     * <p>Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-migrate.png" alt="migrate">
     *
     * @return An object summarising the successfully applied migrations.
     *
     * @throws MigrateDbException when the migration failed.
     */
    public MigrateResult migrate() throws MigrateDbException {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<MigrateResult>() {
            @Override
            public MigrateResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                         Database database,
                                         Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                         StatementInterceptor statementInterceptor) {
                if (configuration.isValidateOnMigrate()) {
                    ValidateResult validateResult = doValidate(database,
                                                               migrationResolver,
                                                               schemaHistory,
                                                               defaultSchema,
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

                    if (!nonEmptySchemas.isEmpty() && !configuration.isSkipExecutingMigrations()) {
                        if (configuration.isBaselineOnMigrate()) {
                            doBaseline(schemaHistory, callbackExecutor, database);
                        } else {
                            // Second check for MySQL which is sometimes flaky otherwise
                            if (!schemaHistory.exists()) {
                                throw new MigrateDbException("Found non-empty schema(s) " +
                                                             StringUtils.collectionToCommaDelimitedString(
                                                                 nonEmptySchemas) +
                                                             " but no schema history table. Use baseline()" +
                                                             " or set baselineOnMigrate to true to initialize the " +
                                                             "schema history table.");
                            }
                        }
                    } else {
                        if (configuration.getCreateSchemas()) {
                            new DbSchemas(database, schemas, schemaHistory, callbackExecutor).create(false);
                        } else if (!defaultSchema.exists()) {
                            LOG.warn("The configuration option 'createSchemas' is false.\n" +
                                     "However, the schema history table still needs a schema to reside in.\n" +
                                     "You must manually create a schema for the schema history table to reside in.\n" +
                                     "See " + WebsiteLinks.CREATE_SCHEMAS);
                        }

                        schemaHistory.create(false);
                    }
                }

                MigrateResult result = new DbMigrate(database,
                                                     schemaHistory,
                                                     defaultSchema,
                                                     migrationResolver,
                                                     configuration,
                                                     callbackExecutor).migrate();

                callbackExecutor.onOperationFinishEvent(Event.AFTER_MIGRATE_OPERATION_FINISH, result);

                return result;
            }
        }, true);
    }

    /**
     * <p>Retrieves the complete information about all the migrations including applied, pending and current migrations
     * with details and status.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-info.png" alt="info">
     *
     * @return All migrations sorted by version, oldest first.
     *
     * @throws MigrateDbException when the info retrieval failed.
     */
    public MigrationInfoService info() {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<MigrationInfoService>() {
            @Override
            public MigrationInfoService execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                                Database database,
                                                Schema defaultSchema, Schema[] schemas,
                                                CallbackExecutor callbackExecutor,
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
     * <p>Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
     * The schemas are cleaned in the order specified by the {@code schemas} property.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-clean.png" alt="clean">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the clean fails.
     */
    public CleanResult clean() {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<CleanResult>() {
            @Override
            public CleanResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                       Database database,
                                       Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                       StatementInterceptor statementInterceptor) {
                CleanResult cleanResult = doClean(database, schemaHistory, defaultSchema, schemas, callbackExecutor);

                callbackExecutor.onOperationFinishEvent(Event.AFTER_CLEAN_OPERATION_FINISH, cleanResult);

                return cleanResult;
            }
        }, false);
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
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-validate.png" alt="validate">
     *
     * @throws MigrateDbException when the validation failed.
     */
    public void validate() throws MigrateDbException {
        migratedbExecutor.execute(new MigrateDbExecutor.Command<Void>() {
            @Override
            public Void execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory, Database database,
                                Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                StatementInterceptor statementInterceptor) {
                ValidateResult validateResult = doValidate(database,
                                                           migrationResolver,
                                                           schemaHistory,
                                                           defaultSchema,
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
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-validate.png" alt="validate">
     *
     * @throws MigrateDbException when the validation failed.
     * @returns An object summarising the validation results
     */
    public ValidateResult validateWithResult() throws MigrateDbException {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<ValidateResult>() {
            @Override
            public ValidateResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                          Database database,
                                          Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                          StatementInterceptor statementInterceptor) {
                ValidateResult validateResult = doValidate(database,
                                                           migrationResolver,
                                                           schemaHistory,
                                                           defaultSchema,
                                                           schemas,
                                                           callbackExecutor,
                                                           configuration.isIgnorePendingMigrations());

                callbackExecutor.onOperationFinishEvent(Event.AFTER_VALIDATE_OPERATION_FINISH, validateResult);

                return validateResult;
            }
        }, true);
    }

    /**
     * <p>Baselines an existing database, excluding all migrations up to and including baselineVersion.</p>
     *
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-baseline.png" alt="baseline">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the schema baselining failed.
     */
    public BaselineResult baseline() throws MigrateDbException {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<BaselineResult>() {
            @Override
            public BaselineResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                          Database database,
                                          Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                          StatementInterceptor statementInterceptor) {
                if (configuration.getCreateSchemas()) {
                    new DbSchemas(database, schemas, schemaHistory, callbackExecutor).create(true);
                } else {
                    LOG.warn("The configuration option 'createSchemas' is false.\n" +
                             "Even though MigrateDb is configured not to create any schemas, the schema history table" +
                             " still needs a schema to reside in.\n" +
                             "You must manually create a schema for the schema history table to reside in.\n" + "See " +
                             WebsiteLinks.CREATE_SCHEMAS);
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
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-repair.png" alt="repair">
     *
     * @return An object summarising the actions taken
     *
     * @throws MigrateDbException when the schema history table repair failed.
     */
    public RepairResult repair() throws MigrateDbException {
        return migratedbExecutor.execute(new MigrateDbExecutor.Command<RepairResult>() {
            @Override
            public RepairResult execute(MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                                        Database database,
                                        Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
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

    private CleanResult doClean(Database database, SchemaHistory schemaHistory, Schema defaultSchema, Schema[] schemas,
                                CallbackExecutor callbackExecutor) {
        return new DbClean(database, schemaHistory, defaultSchema, schemas, callbackExecutor, configuration).clean();
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
                                      Schema defaultSchema, Schema[] schemas, CallbackExecutor callbackExecutor,
                                      boolean ignorePending) {
        ValidateResult validateResult = new DbValidate(database,
                                                       schemaHistory,
                                                       defaultSchema,
                                                       migrationResolver,
                                                       configuration,
                                                       ignorePending,
                                                       callbackExecutor).validate();

        if (!validateResult.validationSuccessful && configuration.isCleanOnValidationError()) {
            doClean(database, schemaHistory, defaultSchema, schemas, callbackExecutor);
        }

        return validateResult;
    }

    private BaselineResult doBaseline(SchemaHistory schemaHistory, CallbackExecutor callbackExecutor,
                                      Database database) {
        return new DbBaseline(schemaHistory,
                              configuration.getBaselineVersion(),
                              configuration.getBaselineDescription(),
                              callbackExecutor,
                              database).baseline();
    }
}
