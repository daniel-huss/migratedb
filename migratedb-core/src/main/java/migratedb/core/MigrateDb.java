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

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrateDbValidateException;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.ClassicConfiguration;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.*;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.command.*;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.StringUtils;
import migratedb.core.internal.util.WebsiteLinks;

import java.util.ArrayList;
import java.util.List;

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
    private final MigrateDbExecutor executor;

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being
     * loaded into a new MigrateDb instance using the load() method.
     * <p>In its simplest form, this is how you configure MigrateDb with all defaults to get started:</p>
     * <pre>MigrateDb migratedb = MigrateDb.configure().dataSource(url, user, password).load();</pre>
     * <p>After that you have a fully-configured MigrateDb instance at your disposal which can be used to invoke
     * MigrateDB functionality such as migrate() or clean().</p>
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
     * @return A new configuration from which MigrateDB can be loaded.
     */
    public static FluentConfiguration configure(ClassLoader classLoader) {
        return new FluentConfiguration(classLoader);
    }

    /**
     * Creates a new instance of MigrateDB with this configuration. In general the MigrateDb.configure() factory method
     * should be preferred over this constructor, unless you need to create or reuse separate Configuration objects.
     *
     * @param configuration The configuration to use.
     */
    public MigrateDb(Configuration configuration) {
        this.configuration = new ClassicConfiguration(configuration);
        this.executor = new MigrateDbExecutor(configuration);
    }

    /**
     * @return The configuration that MigrateDB is using.
     */
    public Configuration getConfiguration() {
        return new ClassicConfiguration(configuration);
    }

    public LiberateResult liberate() {
        return executor.execute(context -> {
            var liberateResult = new DbLiberate(context.schemaHistory,
                    configuration,
                    context.database,
                    context.defaultSchema,
                    context.schemas,
                    context.callbackExecutor)
                    .liberate();
            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_LIBERATE_OPERATION_FINISH, liberateResult);
            return liberateResult;
        }, false);
    }

    /**
     * <p>Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-migrate.png" alt="migrate">
     *
     * @return An object summarising the successfully applied migrations.
     * @throws MigrateDbException when the migration failed.
     */
    public MigrateResult migrate() {
        return executor.execute(context -> {
            if (configuration.isValidateOnMigrate()) {
                ValidateResult validateResult = doValidate(context.database,
                        context.migrationResolver,
                        context.schemaHistory,
                        context.defaultSchema,
                        context.schemas,
                        context.callbackExecutor,
                        true);
                if (!validateResult.validationSuccessful && !configuration.isCleanOnValidationError()) {
                    throw new MigrateDbValidateException(validateResult.errorDetails,
                            validateResult.getAllErrorMessages());
                }
            }

            if (!context.schemaHistory.exists()) {
                List<Schema<?, ?>> nonEmptySchemas = new ArrayList<>();
                for (var schema : context.schemas) {
                    if (schema.exists() && !schema.empty()) {
                        nonEmptySchemas.add(schema);
                    }
                }

                if (!nonEmptySchemas.isEmpty() && !configuration.isSkipExecutingMigrations()) {
                    if (configuration.isBaselineOnMigrate()) {
                        doBaseline(context.schemaHistory, context.callbackExecutor, context.database);
                    } else {
                        // Second check for MySQL which is sometimes flaky otherwise
                        if (!context.schemaHistory.exists()) {
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
                        new DbSchemas(context.database,
                                context.schemas,
                                context.schemaHistory,
                                context.callbackExecutor).create(false);
                    } else if (!context.defaultSchema.exists()) {
                        LOG.warn("The configuration option 'createSchemas' is false.\n" +
                                "However, the schema history table still needs a schema to reside in.\n" +
                                "You must manually create a schema for the schema history table to reside in.\n" +
                                "See " + WebsiteLinks.CREATE_SCHEMAS);
                    }

                    context.schemaHistory.create(false);
                }
            }

            MigrateResult result = new DbMigrate(context.database,
                    context.schemaHistory,
                    context.defaultSchema,
                    context.migrationResolver,
                    configuration,
                    context.callbackExecutor).migrate();

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_MIGRATE_OPERATION_FINISH, result);

            return result;
        }, true);
    }

    /**
     * <p>Retrieves the complete information about all the migrations including applied, pending and current migrations
     * with details and status.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-info.png" alt="info">
     *
     * @return All migrations sorted by version, oldest first.
     * @throws MigrateDbException when the info retrieval failed.
     */
    public MigrationInfoService info() {
        return executor.execute(context -> {
            MigrationInfoService migrationInfoService = new DbInfo(context.migrationResolver,
                    context.schemaHistory,
                    configuration,
                    context.database,
                    context.callbackExecutor,
                    context.schemas).info();

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_INFO_OPERATION_FINISH,
                    migrationInfoService.getInfoResult());

            return migrationInfoService;
        }, true);
    }

    /**
     * <p>Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
     * The schemas are cleaned in the order specified by the {@code schemas} property.</p>
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-clean.png" alt="clean">
     *
     * @return An object summarising the actions taken
     * @throws MigrateDbException when the clean fails.
     */
    public CleanResult clean() {
        return executor.execute(context -> {
            CleanResult cleanResult = doClean(context.database,
                    context.schemaHistory,
                    context.defaultSchema,
                    context.schemas,
                    context.callbackExecutor);

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_CLEAN_OPERATION_FINISH, cleanResult);

            return cleanResult;
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
        executor.<Void>execute(context -> {
            ValidateResult validateResult = doValidate(context.database,
                    context.migrationResolver,
                    context.schemaHistory,
                    context.defaultSchema,
                    context.schemas,
                    context.callbackExecutor,
                    configuration.isIgnorePendingMigrations());

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_VALIDATE_OPERATION_FINISH, validateResult);

            if (!validateResult.validationSuccessful && !configuration.isCleanOnValidationError()) {
                throw new MigrateDbValidateException(validateResult.errorDetails,
                        validateResult.getAllErrorMessages());
            }

            return null;
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
     * @return An object summarising the validation results
     * @throws MigrateDbException when the validation failed.
     */
    public ValidateResult validateWithResult() throws MigrateDbException {
        return executor.execute(context -> {
            ValidateResult validateResult = doValidate(context.database,
                    context.migrationResolver,
                    context.schemaHistory,
                    context.defaultSchema,
                    context.schemas,
                    context.callbackExecutor,
                    configuration.isIgnorePendingMigrations());

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_VALIDATE_OPERATION_FINISH, validateResult);

            return validateResult;
        }, true);
    }

    /**
     * <p>Baselines an existing database, excluding all migrations up to and including baselineVersion.</p>
     *
     * <img src="https://daniel-huss.github.io/migratedb/assets/balsamiq/command-baseline.png" alt="baseline">
     *
     * @return An object summarising the actions taken
     * @throws MigrateDbException when the schema baselining failed.
     */
    public BaselineResult baseline() throws MigrateDbException {
        return executor.execute(context -> {
            if (configuration.getCreateSchemas()) {
                new DbSchemas(context.database,
                        context.schemas,
                        context.schemaHistory,
                        context.callbackExecutor).create(true);
            } else {
                LOG.warn("The configuration option 'createSchemas' is false.\n" +
                        "Even though MigrateDb is configured not to create any schemas, the schema history table" +
                        " still needs a schema to reside in.\n" +
                        "You must manually create a schema for the schema history table to reside in.\n" + "See " +
                        WebsiteLinks.CREATE_SCHEMAS);
            }

            BaselineResult baselineResult = doBaseline(context.schemaHistory,
                    context.callbackExecutor,
                    context.database);

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_BASELINE_OPERATION_FINISH, baselineResult);

            return baselineResult;
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
     * @throws MigrateDbException when the schema history table repair failed.
     */
    public RepairResult repair() throws MigrateDbException {
        return executor.execute(context -> {
            RepairResult repairResult = new DbRepair(context.database,
                    context.migrationResolver,
                    context.schemaHistory,
                    context.callbackExecutor,
                    configuration).repair();

            context.callbackExecutor.onOperationFinishEvent(Event.AFTER_REPAIR_OPERATION_FINISH, repairResult);

            return repairResult;
        }, true);
    }

    private CleanResult doClean(Database<?> database,
                                SchemaHistory schemaHistory,
                                Schema<?, ?> defaultSchema,
                                Schema<?, ?>[] schemas,
                                CallbackExecutor callbackExecutor) {
        return new DbClean(database, schemaHistory, defaultSchema, schemas, callbackExecutor, configuration).clean();
    }

    /**
     * Performs the actual validation. All set up must have taken place beforehand.
     *
     * @param database          The database-specific support.
     * @param migrationResolver The migration resolver;
     * @param schemaHistory     The schema history table.
     * @param schemas           The schemas managed by MigrateDB.
     * @param callbackExecutor  The callback executor.
     * @param ignorePending     Whether to ignore pending migrations.
     */
    private ValidateResult doValidate(Database<?> database,
                                      MigrationResolver migrationResolver,
                                      SchemaHistory schemaHistory,
                                      Schema<?, ?> defaultSchema,
                                      Schema<?, ?>[] schemas,
                                      CallbackExecutor callbackExecutor,
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

    private BaselineResult doBaseline(SchemaHistory schemaHistory,
                                      CallbackExecutor callbackExecutor,
                                      Database<?> database) {
        return new DbBaseline(schemaHistory,
                configuration.getBaselineVersion(),
                configuration.getBaselineDescription(),
                callbackExecutor,
                database).baseline();
    }
}
