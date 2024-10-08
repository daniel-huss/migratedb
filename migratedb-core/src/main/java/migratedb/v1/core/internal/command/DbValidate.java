/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.command;

import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.ErrorDetails;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.output.CommandResultFactory;
import migratedb.v1.core.api.output.ValidateOutput;
import migratedb.v1.core.api.output.ValidateResult;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.internal.info.MigrationInfoServiceImpl;
import migratedb.v1.core.internal.info.ValidationContext;
import migratedb.v1.core.internal.info.ValidationMatch;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import migratedb.v1.core.internal.util.DateTimeUtils;
import migratedb.v1.core.internal.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

import static migratedb.v1.core.internal.jdbc.ExecutionTemplateFactory.createExecutionTemplate;

/**
 * Handles the validate command.
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
     * The session to use.
     */
    private final Session session;

    /**
     * The current configuration.
     */
    private final Configuration configuration;

    /**
     * Whether pending migrations are allowed.
     */
    private final boolean allowPending;

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
     * @param allowPending      Whether pending migrations are allowed.
     * @param callbackExecutor  The callback executor.
     */
    public DbValidate(Database database,
                      SchemaHistory schemaHistory,
                      Schema schema,
                      MigrationResolver migrationResolver,
                      Configuration configuration,
                      boolean allowPending,
                      CallbackExecutor callbackExecutor) {
        this.database = database;
        this.session = database.getMainSession();
        this.schemaHistory = schemaHistory;
        this.schema = schema;
        this.migrationResolver = migrationResolver;
        this.configuration = configuration;
        this.allowPending = allowPending;
        this.callbackExecutor = callbackExecutor;
    }

    private static class CountAndInvalidMigrations {
        public final int count;
        public final List<ValidateOutput> invalidMigrations;

        private CountAndInvalidMigrations(int count,
                                          List<ValidateOutput> invalidMigrations) {
            this.count = count;
            this.invalidMigrations = invalidMigrations;
        }
    }

    /**
     * Starts the actual migration.
     *
     * @return The validation error, if any.
     */
    public ValidateResult validate() {
        if (!schema.exists()) {
            if (!migrationResolver.resolveMigrations(() -> configuration).isEmpty() && !allowPending) {
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

        var result = createExecutionTemplate(session.getJdbcConnection(), database).execute(() -> {
            var validationContext = new ValidationContext(configuration)
                    .with(ValidationMatch.PENDING, allowPending);
            var migrationInfoService = new MigrationInfoServiceImpl(migrationResolver,
                                                                    schemaHistory,
                                                                    database,
                                                                    configuration,
                                                                    configuration.getTarget(),
                                                                    configuration.getCherryPick(),
                                                                    validationContext);

            migrationInfoService.refresh();

            int count = migrationInfoService.all().length;
            List<ValidateOutput> invalidMigrations = migrationInfoService.validate();
            return new CountAndInvalidMigrations(count, invalidMigrations);
        });

        stopWatch.stop();

        List<String> warnings = new ArrayList<>();
        List<ValidateOutput> invalidMigrations = result.invalidMigrations;
        ErrorDetails validationError = null;
        int count = 0;
        if (invalidMigrations.isEmpty()) {
            count = result.count;
            if (count == 1) {
                LOG.info(String.format("Successfully validated 1 migration (execution time %s)",
                                       DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));
            } else {
                LOG.info(String.format("Successfully validated %d migrations (execution time %s)",
                                       count, DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));

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
