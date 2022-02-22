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
package migratedb.core.internal.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.ErrorDetails;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.api.output.ValidateOutput;
import migratedb.core.api.output.ValidateResult;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.callback.CallbackExecutor;
import migratedb.core.internal.database.base.Connection;
import migratedb.core.internal.database.base.Database;
import migratedb.core.internal.database.base.Schema;
import migratedb.core.internal.info.MigrationInfoServiceImpl;
import migratedb.core.internal.jdbc.ExecutionTemplateFactory;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.Pair;
import migratedb.core.internal.util.StopWatch;
import migratedb.core.internal.util.TimeFormat;

/**
 * Handles the validate command.
 *
 * @author Axel Fontaine
 */
public class DbValidate {
    private static final Log LOG = Log.getLog(DbValidate.class);

    /**
     * The database schema history table.
     */
    private final SchemaHistory schemaHistory;

    /**
     * The schema containing the schema history table.
     */
    private final Schema schema;

    /**
     * The migration resolver.
     */
    private final MigrationResolver migrationResolver;

    /**
     * The connection to use.
     */
    private final Connection connection;

    /**
     * The current configuration.
     */
    private final Configuration configuration;

    /**
     * Whether pending migrations are allowed.
     */
    private final boolean pending;

    /**
     * The callback executor.
     */
    private final CallbackExecutor callbackExecutor;

    /**
     * The database-specific support.
     */
    private final Database database;

    /**
     * Creates a new database validator.
     *
     * @param database          The DB support for the connection.
     * @param schemaHistory     The database schema history table.
     * @param schema            The schema containing the schema history table.
     * @param migrationResolver The migration resolver.
     * @param configuration     The current configuration.
     * @param pending           Whether pending migrations are allowed.
     * @param callbackExecutor  The callback executor.
     */
    public DbValidate(Database database, SchemaHistory schemaHistory, Schema schema,
                      MigrationResolver migrationResolver,
                      Configuration configuration, boolean pending, CallbackExecutor callbackExecutor) {
        this.database = database;
        this.connection = database.getMainConnection();
        this.schemaHistory = schemaHistory;
        this.schema = schema;
        this.migrationResolver = migrationResolver;
        this.configuration = configuration;
        this.pending = pending;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Starts the actual migration.
     *
     * @return The validation error, if any.
     */
    public ValidateResult validate() {
        if (!schema.exists()) {
            if (!migrationResolver.resolveMigrations(new Context() {
                @Override
                public Configuration getConfiguration() {
                    return configuration;
                }
            }).isEmpty() && !pending) {
                String validationErrorMessage = "Schema " + schema + " doesn't exist yet";
                ErrorDetails validationError = new ErrorDetails(ErrorCode.SCHEMA_DOES_NOT_EXIST,
                                                                validationErrorMessage);
                return CommandResultFactory.createValidateResult(database.getCatalog(),
                                                                 validationError,
                                                                 0,
                                                                 null,
                                                                 new ArrayList<>());
            }
            return CommandResultFactory.createValidateResult(database.getCatalog(), null, 0, null, new ArrayList<>());
        }

        callbackExecutor.onEvent(Event.BEFORE_VALIDATE);

        LOG.debug("Validating migrations ...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Pair<Integer, List<ValidateOutput>> result =
            ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(),
                                                                                                      database).execute(
            new Callable<Pair<Integer, List<ValidateOutput>>>() {
                @Override
                public Pair<Integer, List<ValidateOutput>> call() {
                    MigrationInfoServiceImpl migrationInfoService =
                        new MigrationInfoServiceImpl(migrationResolver, schemaHistory, database, configuration,
                                                     configuration.getTarget(),
                                                     configuration.isOutOfOrder(),
                                                     configuration.getCherryPick(),
                                                     pending,
                                                     configuration.isIgnoreMissingMigrations(),
                                                     configuration.isIgnoreIgnoredMigrations(),
                                                     configuration.isIgnoreFutureMigrations());

                    migrationInfoService.refresh();

                    int count = migrationInfoService.all().length;
                    List<ValidateOutput> invalidMigrations = migrationInfoService.validate();
                    return Pair.of(count, invalidMigrations);
                }
            });

        stopWatch.stop();

        List<String> warnings = new ArrayList<>();
        List<ValidateOutput> invalidMigrations = result.getRight();
        ErrorDetails validationError = null;
        int count = 0;
        if (invalidMigrations.isEmpty()) {
            count = result.getLeft();
            if (count == 1) {
                LOG.info(String.format("Successfully validated 1 migration (execution time %s)",
                                       TimeFormat.format(stopWatch.getTotalTimeMillis())));
            } else {
                LOG.info(String.format("Successfully validated %d migrations (execution time %s)",
                                       count, TimeFormat.format(stopWatch.getTotalTimeMillis())));

                if (count == 0) {
                    String noMigrationsWarning = "No migrations found. Are your locations set up correctly?";
                    warnings.add(noMigrationsWarning);
                    LOG.warn(noMigrationsWarning);
                }
            }
            callbackExecutor.onEvent(Event.AFTER_VALIDATE);
        } else {
            validationError = new ErrorDetails(ErrorCode.VALIDATE_ERROR, "Migrations have failed validation");
            callbackExecutor.onEvent(Event.AFTER_VALIDATE_ERROR);
        }

        return CommandResultFactory.createValidateResult(database.getCatalog(),
                                                         validationError,
                                                         count,
                                                         invalidMigrations,
                                                         warnings);
    }
}