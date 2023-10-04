/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.MigrationInfo;
import migratedb.v1.core.api.MigrationState;
import migratedb.v1.core.api.MigrationState.Category;
import migratedb.v1.core.api.TargetVersion;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.schemahistory.AppliedMigration;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.output.CommandResultFactory;
import migratedb.v1.core.api.output.CompletedRepairActions;
import migratedb.v1.core.api.output.RepairResult;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.internal.info.MigrationInfoServiceImpl;
import migratedb.v1.core.internal.info.ValidationContext;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import migratedb.v1.core.internal.util.DateTimeUtils;
import migratedb.v1.core.internal.util.StopWatch;

import java.util.Objects;

import static migratedb.v1.core.internal.jdbc.ExecutionTemplateFactory.createExecutionTemplate;

/**
 * Handles MigrateDB's repair command.
 */
public class DbRepair {
    private static final Log LOG = Log.getLog(DbRepair.class);

    /**
     * The database connection to use for accessing the schema history table.
     */
    private final Session connection;

    /**
     * The migration infos.
     */
    private final MigrationInfoServiceImpl migrationInfoService;

    /**
     * The schema history table.
     */
    private final SchemaHistory schemaHistory;

    /**
     * The callback executor.
     */
    private final CallbackExecutor callbackExecutor;

    /**
     * The database-specific support.
     */
    private final Database database;

    /**
     * The POJO containing the repair result.
     */
    private final RepairResult repairResult;

    /**
     * The MigrateDB configuration.
     */
    private final Configuration configuration;

    /**
     * Creates a new DbRepair.
     *
     * @param database          The database-specific support.
     * @param migrationResolver The migration resolver.
     * @param schemaHistory     The schema history table.
     * @param callbackExecutor  The callback executor.
     */
    public DbRepair(Database database, MigrationResolver migrationResolver, SchemaHistory schemaHistory,
                    CallbackExecutor callbackExecutor, Configuration configuration) {
        this.database = database;
        this.connection = database.getMainConnection();
        this.schemaHistory = schemaHistory;
        this.callbackExecutor = callbackExecutor;
        this.configuration = configuration;

        this.migrationInfoService = new MigrationInfoServiceImpl(
                migrationResolver,
                schemaHistory,
                database,
                configuration,
                TargetVersion.LATEST,
                configuration.getCherryPick(),
                ValidationContext.allAllowed());

        this.repairResult = CommandResultFactory.createRepairResult(configuration, database);
    }

    /**
     * Repairs the schema history table.
     */
    public RepairResult repair() {
        callbackExecutor.onEvent(Event.BEFORE_REPAIR);

        CompletedRepairActions repairActions;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            repairActions = createExecutionTemplate(connection.getJdbcConnection(), database)
                    .execute(() -> {
                        CompletedRepairActions completedActions = new CompletedRepairActions();

                        completedActions.removedFailedMigrations = schemaHistory.removeFailedMigrations(
                                repairResult,
                                configuration.getCherryPick());
                        migrationInfoService.refresh();

                        completedActions.deletedMissingMigrations = markUnavailableMigrationsAsDeleted();

                        completedActions.alignedAppliedMigrationChecksums = alignAppliedMigrationsWithResolvedMigrations();
                        return completedActions;
                    });

            stopWatch.stop();

            LOG.info("Successfully repaired schema history table " + schemaHistory + " (execution time "
                     + DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis()) + ").");
            if (repairActions.deletedMissingMigrations) {
                LOG.info(
                        "Please ensure the previous contents of the deleted migrations are removed from the database, or " +
                        "moved into an existing migration.");
            }
            if (repairActions.removedFailedMigrations && !database.supportsDdlTransactions()) {
                LOG.info("Manual cleanup of the remaining effects of the failed migration may still be required.");
            }
        } catch (MigrateDbException e) {
            callbackExecutor.onEvent(Event.AFTER_REPAIR_ERROR);
            throw e;
        }

        callbackExecutor.onEvent(Event.AFTER_REPAIR);

        repairResult.setRepairActions(repairActions);
        return repairResult;
    }

    private boolean markUnavailableMigrationsAsDeleted() {
        boolean removed = false;
        for (MigrationInfo migrationInfo : migrationInfoService.all()) {
            if (migrationInfo.getType().isExclusiveToAppliedMigrations()) {
                continue;
            }

            AppliedMigration applied = migrationInfo.getAppliedMigration();
            MigrationState state = migrationInfo.getState();
            boolean shouldDelete = state.is(Category.MISSING) && !state.is(Category.FUTURE);
            boolean isIgnoredByPattern = configuration.getIgnoreMigrationPatterns()
                                                      .stream()
                                                      .anyMatch(p -> p.matchesMigration(migrationInfo.getVersion() != null, state));
            if (shouldDelete && !isIgnoredByPattern) {
                schemaHistory.delete(applied);
                removed = true;
                repairResult.migrationsDeleted.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }
        }

        return removed;
    }

    private boolean alignAppliedMigrationsWithResolvedMigrations() {
        boolean repaired = false;
        for (MigrationInfo migrationInfo : migrationInfoService.all()) {
            ResolvedMigration resolved = migrationInfo.getResolvedMigration();
            AppliedMigration applied = migrationInfo.getAppliedMigration();

            // Repair versioned
            if (resolved != null
                && resolved.getVersion() != null
                && applied != null
                && !applied.getType().isExclusiveToAppliedMigrations()
                && migrationInfo.getState() != MigrationState.IGNORED
                && updateNeeded(resolved, applied)) {
                schemaHistory.update(applied, resolved);
                repaired = true;
                repairResult.migrationsAligned.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }

            // Repair repeatable
            if (resolved != null
                && resolved.getVersion() == null
                && applied != null
                && !applied.getType().isExclusiveToAppliedMigrations()
                && migrationInfo.getState() != MigrationState.IGNORED
                && resolved.checksumMatches(applied.getChecksum())) {
                schemaHistory.update(applied, resolved);
                repaired = true;
                repairResult.migrationsAligned.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }
        }

        return repaired;
    }

    private boolean updateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return checksumUpdateNeeded(resolved, applied)
               || descriptionUpdateNeeded(resolved, applied)
               || typeUpdateNeeded(resolved, applied);
    }

    private boolean checksumUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return !resolved.checksumMatches(applied.getChecksum());
    }

    private boolean descriptionUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        if (!database.supportsEmptyMigrationDescription() && "".equals(resolved.getDescription())) {
            return !Objects.equals(SchemaHistory.NO_DESCRIPTION_MARKER, applied.getDescription());
        }
        return !Objects.equals(resolved.getDescription(), applied.getDescription());
    }

    private boolean typeUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return !Objects.equals(resolved.getType(), applied.getType());
    }

}
