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

import migratedb.v1.core.api.*;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.executor.Context;
import migratedb.v1.core.api.executor.MigrationExecutor;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.output.CommandResultFactory;
import migratedb.v1.core.api.output.MigrateResult;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.internal.info.MigrationInfoServiceImpl;
import migratedb.v1.core.internal.info.ValidationContext;
import migratedb.v1.core.internal.info.ValidationMatch;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import migratedb.v1.core.internal.util.DateTimeUtils;
import migratedb.v1.core.internal.util.ExceptionUtils;
import migratedb.v1.core.internal.util.StopWatch;
import migratedb.v1.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static migratedb.v1.core.internal.jdbc.ExecutionTemplateFactory.createExecutionTemplate;

public class DbMigrate {
    private static final Log LOG = Log.getLog(DbMigrate.class);

    private final Database database;
    private final SchemaHistory schemaHistory;
    /**
     * The schema containing the schema history table.
     */
    private final Schema schema;
    private final MigrationResolver migrationResolver;
    private final Configuration configuration;
    private final CallbackExecutor callbackExecutor;
    /**
     * The session to use to perform the actual database migrations.
     */
    private final Session session;
    private MigrateResult migrateResult;
    /**
     * This is used to remember the type of migration between calls to migrateGroup().
     */
    private boolean isPreviousVersioned;
    private final List<ResolvedMigration> appliedResolvedMigrations = new ArrayList<>();

    public DbMigrate(Database database,
                     SchemaHistory schemaHistory,
                     Schema schema,
                     MigrationResolver migrationResolver,
                     Configuration configuration,
                     CallbackExecutor callbackExecutor) {
        this.database = database;
        this.session = database.getMigrationSession();
        this.schemaHistory = schemaHistory;
        this.schema = schema;
        this.migrationResolver = migrationResolver;
        this.configuration = configuration;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Starts the actual migration.
     */
    public MigrateResult migrate() throws MigrateDbException {
        callbackExecutor.onMigrateEvent(Event.BEFORE_MIGRATE);

        migrateResult = CommandResultFactory.createMigrateResult(database.getCatalog(), configuration);

        int count;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            count = configuration.isGroup() ?
                    // When group is active, start the transaction boundary early to
                    // ensure that all changes to the schema history table are either committed or rolled back
                    // atomically.
                    schemaHistory.withLock(this::migrateAll) :
                    // For all regular cases, proceed with the migration as usual.
                    migrateAll();

            stopWatch.stop();

            migrateResult.targetSchemaVersion = getTargetVersion();
            migrateResult.migrationsExecuted = count;

            logSummary(count, stopWatch.getTotalTimeMillis(), migrateResult.targetSchemaVersion);
        } catch (MigrateDbException e) {
            callbackExecutor.onMigrateEvent(Event.AFTER_MIGRATE_ERROR);
            throw e;
        }

        if (count > 0) {
            callbackExecutor.onMigrateEvent(Event.AFTER_MIGRATE_APPLIED);
        }
        callbackExecutor.onMigrateEvent(Event.AFTER_MIGRATE);

        return migrateResult;
    }

    private String getTargetVersion() {
        if (!migrateResult.migrations.isEmpty()) {
            for (int i = migrateResult.migrations.size() - 1; i >= 0; i--) {
                String targetVersion = migrateResult.migrations.get(i).version;
                if (!targetVersion.isEmpty()) {
                    return targetVersion;
                }
            }
        }
        return null;
    }

    private int migrateAll() {
        int total = 0;
        isPreviousVersioned = true;

        while (true) {
            boolean firstRun = total == 0;
            int count = configuration.isGroup()
                    // With group active a lock on the schema history table has already been acquired.
                    ? migrateGroup(firstRun)
                    // Otherwise acquire the lock now. The lock will be released at the end of each migration.
                    : schemaHistory.withLock(() -> migrateGroup(firstRun));
            total += count;
            if (count == 0) {
                // No further migrations available
                break;
            } else if (Objects.equals(configuration.getTarget(), TargetVersion.NEXT)) {
                // With target=next we only execute one migration
                break;
            }
        }

        if (isPreviousVersioned) {
            callbackExecutor.onMigrateEvent(Event.AFTER_VERSIONED);
        }

        return total;
    }

    /**
     * Migrate a group of one (group = false) or more (group = true) migrations.
     *
     * @param firstRun Whether this is the first time this code runs in this migration run.
     * @return The number of newly applied migrations.
     */
    private Integer migrateGroup(boolean firstRun) {
        var allowedMatches = EnumSet.allOf(ValidationMatch.class);
        if (!configuration.isOutOfOrder()) {
            allowedMatches.remove(ValidationMatch.OUT_OF_ORDER);
        }
        var infoService = new MigrationInfoServiceImpl(migrationResolver,
                                                       schemaHistory,
                                                       database,
                                                       configuration,
                                                       configuration.getTarget(),
                                                       configuration.getCherryPick(),
                                                       new ValidationContext(allowedMatches));
        infoService.refresh();

        var current = infoService.current();
        var currentSchemaVersion = current == null ? null : current.getVersion();
        var currentSchemaVersionString = currentSchemaVersion == null ? SchemaHistory.EMPTY_SCHEMA_DESCRIPTION
                : currentSchemaVersion.toString();
        if (firstRun) {
            LOG.info("Current version of schema " + schema + ": " + currentSchemaVersionString);

            migrateResult.initialSchemaVersion = currentSchemaVersionString;
            if (configuration.isOutOfOrder()) {
                String outOfOrderWarning =
                        "outOfOrder mode is active. Migration of schema " + schema + " may not be reproducible.";
                LOG.warn(outOfOrderWarning);
                migrateResult.addWarning(outOfOrderWarning);
            }
        }

        MigrationInfo[] future = infoService.future();
        if (future.length > 0) {
            List<MigrationInfo> resolved = Arrays.asList(infoService.resolved());
            Collections.reverse(resolved);
            if (resolved.isEmpty()) {
                LOG.error("Schema " + schema + " has version " + currentSchemaVersionString
                          + ", but no migration could be resolved in the configured locations !");
            } else {
                for (MigrationInfo migrationInfo : resolved) {
                    // Only consider versioned migrations
                    if (migrationInfo.getVersion() != null) {
                        LOG.warn("Schema " + schema + " has a version (" + currentSchemaVersionString
                                 + ") that is newer than the latest available migration ("
                                 + migrationInfo.getVersion() + ") !");
                        break;
                    }
                }
            }
        }

        MigrationInfo[] failed = infoService.failed();
        if (failed.length > 0) {
            if ((failed.length == 1)
                && (failed[0].getState() == MigrationState.FUTURE_FAILED)
                && configuration.isIgnoreFutureMigrations()) {
                LOG.warn(
                        "Schema " + schema + " contains a failed future migration to version " + failed[0].getVersion() +
                        " !");
            } else {
                if (failed[0].getVersion() == null) {
                    throw new MigrateDbException("Schema " + schema + " contains a failed repeatable migration (" +
                                                 doQuote(failed[0].getDescription()) + ") !");
                }
                throw new MigrateDbException(
                        "Schema " + schema + " contains a failed migration to version " + failed[0].getVersion() + " !");
            }
        }

        Map<MigrationInfo, Boolean> group = new LinkedHashMap<>();
        for (MigrationInfo pendingMigration : infoService.pending()) {
            if (appliedResolvedMigrations.contains(pendingMigration.getResolvedMigration())) {
                continue;
            }

            boolean isOutOfOrder = isOutOfOrder(pendingMigration, currentSchemaVersion);

            group.put(pendingMigration, isOutOfOrder);

            if (!configuration.isGroup()) {
                // Only include one pending migration if group is disabled
                break;
            }
        }

        if (!group.isEmpty()) {
            boolean skipExecutingMigrations = configuration.isSkipExecutingMigrations();
            applyMigrations(group, skipExecutingMigrations);
        }
        return group.size();
    }

    private boolean isOutOfOrder(MigrationInfo pendingMigration, @Nullable Version currentSchemaVersion) {
        var pendingVersion = pendingMigration.getVersion();
        if (pendingVersion == null) {
            return false;
        }
        return currentSchemaVersion != null && pendingVersion.compareTo(currentSchemaVersion) < 0;
    }

    private void logSummary(int migrationSuccessCount, long executionTime, String targetVersion) {
        if (migrationSuccessCount == 0) {
            LOG.info("Schema " + schema + " is up to date. No migration necessary.");
            return;
        }

        String targetText = (targetVersion != null) ? ", now at version v" + targetVersion : "";

        String migrationText = (migrationSuccessCount == 1) ? "migration" : "migrations";

        LOG.info("Successfully applied " + migrationSuccessCount + " " + migrationText + " to schema " + schema
                 + targetText + " (execution time " + DateTimeUtils.formatDuration(executionTime) + ")");
    }

    /**
     * Applies this migration to the database. The migration state and the execution time are updated accordingly.
     */
    private void applyMigrations(Map<MigrationInfo, Boolean> group, boolean skipExecutingMigrations) {
        boolean executeGroupInTransaction = isExecuteGroupInTransaction(group);
        StopWatch stopWatch = new StopWatch();
        try {
            if (executeGroupInTransaction) {
                createExecutionTemplate(session.getJdbcConnection(), database)
                        .execute(() -> {
                            doMigrateGroup(group, stopWatch, skipExecutingMigrations, true);
                            return null;
                        });
            } else {
                doMigrateGroup(group, stopWatch, skipExecutingMigrations, false);
            }
        } catch (MigrateDbMigrateException e) {
            MigrationInfo migration = e.getMigration();
            var resolvedMigration = migration.getResolvedMigration();
            assert resolvedMigration != null;
            String failedMsg = "Migration of " + toMigrationText(migration, e.isOutOfOrder()) + " failed!";
            if (database.supportsDdlTransactions() && executeGroupInTransaction) {
                LOG.error(failedMsg + " Changes successfully rolled back.");
            } else {
                LOG.error(failedMsg + " Please restore backups and roll back database and code!");

                stopWatch.stop();
                int executionTime = (int) stopWatch.getTotalTimeMillis();
                schemaHistory.addAppliedMigration(migration.getVersion(),
                                                  migration.getDescription(),
                                                  migration.getType(),
                                                  migration.getScript(),
                                                  resolvedMigration.getChecksum(),
                                                  executionTime,
                                                  false);
            }
            throw e;
        }
    }

    private boolean isExecuteGroupInTransaction(Map<MigrationInfo, Boolean> group) {
        boolean executeGroupInTransaction = true;
        boolean first = true;

        for (var entry : group.entrySet()) {
            var resolvedMigration = entry.getKey().getResolvedMigration();
            assert resolvedMigration != null;
            boolean inTransaction = resolvedMigration.getExecutor().canExecuteInTransaction();

            if (first) {
                executeGroupInTransaction = inTransaction;
                first = false;
                continue;
            }

            if (!configuration.isMixed() && executeGroupInTransaction != inTransaction) {
                throw new MigrateDbException(
                        "Detected both transactional and non-transactional migrations within the same migration group"
                        + " (even though mixed is false). First offending migration: "
                        + doQuote((resolvedMigration.getVersion() == null ? "" : resolvedMigration.getVersion())
                                  + (StringUtils.hasLength(resolvedMigration.getDescription()) ? " " +
                                                                                                 resolvedMigration.getDescription()
                                : ""))
                        + (inTransaction ? "" : " [non-transactional]"));
            }

            executeGroupInTransaction &= inTransaction;
        }

        return executeGroupInTransaction;
    }

    private void doMigrateGroup(Map<MigrationInfo, Boolean> group, StopWatch stopWatch,
                                boolean skipExecutingMigrations, boolean isExecuteInTransaction) {
        Context context = new Context() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }

            @Override
            public Connection getConnection() {
                return session.getJdbcConnection();
            }
        };

        for (var entry : group.entrySet()) {
            var migrationInfo = entry.getKey();
            var resolvedMigration = migrationInfo.getResolvedMigration();
            assert resolvedMigration != null;
            boolean isOutOfOrder = entry.getValue();

            String migrationText = toMigrationText(migrationInfo, isOutOfOrder);

            stopWatch.start();

            if (isPreviousVersioned && migrationInfo.getVersion() == null) {
                callbackExecutor.onMigrateEvent(Event.AFTER_VERSIONED);
                callbackExecutor.onMigrateEvent(Event.BEFORE_REPEATABLES);
                isPreviousVersioned = false;
            }

            if (skipExecutingMigrations) {
                LOG.debug("Skipping execution of migration of " + migrationText);
            } else {
                LOG.debug("Starting migration of " + migrationText + " ...");

                session.restoreOriginalState();
                session.changeCurrentSchemaTo(schema);

                try {
                    callbackExecutor.setMigrationInfo(migrationInfo);
                    callbackExecutor.onEachMigrateEvent(Event.BEFORE_EACH_MIGRATE);
                    try {
                        LOG.info("Migrating " + migrationText);

                        // With single connection databases we need to manually disable the transaction for the
                        // migration as it is turned on for schema history changes
                        boolean oldAutoCommit = context.getConnection().getAutoCommit();
                        if (database.usesSingleSession() && !isExecuteInTransaction) {
                            context.getConnection().setAutoCommit(true);
                        }
                        resolvedMigration.getExecutor().execute(context);
                        if (database.usesSingleSession() && !isExecuteInTransaction) {
                            context.getConnection().setAutoCommit(oldAutoCommit);
                        }

                        appliedResolvedMigrations.add(resolvedMigration);
                    } catch (MigrateDbException e) {
                        callbackExecutor.onEachMigrateEvent(Event.AFTER_EACH_MIGRATE_ERROR);
                        throw new MigrateDbMigrateException(migrationInfo, isOutOfOrder, e);
                    } catch (SQLException e) {
                        callbackExecutor.onEachMigrateEvent(Event.AFTER_EACH_MIGRATE_ERROR);
                        throw new MigrateDbMigrateException(migrationInfo, isOutOfOrder, e);
                    }

                    LOG.debug("Successfully completed migration of " + migrationText);
                    callbackExecutor.onEachMigrateEvent(Event.AFTER_EACH_MIGRATE);
                } finally {
                    callbackExecutor.setMigrationInfo(null);
                }
            }

            stopWatch.stop();
            int executionTime = (int) stopWatch.getTotalTimeMillis();

            migrateResult.migrations.add(CommandResultFactory.createMigrateOutput(migrationInfo, executionTime));

            schemaHistory.addAppliedMigration(migrationInfo.getVersion(),
                                              migrationInfo.getDescription(),
                                              migrationInfo.getType(),
                                              migrationInfo.getScript(),
                                              resolvedMigration.getChecksum(),
                                              executionTime,
                                              true);
        }
    }

    private String toMigrationText(MigrationInfo migration, boolean isOutOfOrder) {
        var resolvedMigration = migration.getResolvedMigration();
        assert resolvedMigration != null;
        MigrationExecutor migrationExecutor = resolvedMigration.getExecutor();
        String migrationText;
        if (migration.getVersion() != null) {
            migrationText = "schema " + schema + " to version " + doQuote(migration.getVersion()
                                                                          +
                                                                          (StringUtils.hasLength(migration.getDescription())
                                                                                  ? " - " + migration.getDescription() : ""))
                            + (isOutOfOrder ? " [out of order]" : "")
                            + (migrationExecutor.canExecuteInTransaction() ? "" : " [non-transactional]");
        } else {
            migrationText = "schema " + schema + " with repeatable migration " + doQuote(migration.getDescription())
                            + (migrationExecutor.canExecuteInTransaction() ? "" : " [non-transactional]");
        }
        return migrationText;
    }

    private String doQuote(String text) {
        return "\"" + text + "\"";
    }

    public static class MigrateDbMigrateException extends MigrateDbException {
        private final MigrationInfo migration;
        private final boolean outOfOrder;

        MigrateDbMigrateException(MigrationInfo migration, boolean outOfOrder, SQLException e) {
            super(ExceptionUtils.toMessage(e), e);
            this.migration = migration;
            this.outOfOrder = outOfOrder;
        }

        MigrateDbMigrateException(MigrationInfo migration, boolean outOfOrder, MigrateDbException e) {
            super(e.getMessage(), e);
            this.migration = migration;
            this.outOfOrder = outOfOrder;
        }

        public MigrationInfo getMigration() {
            return migration;
        }

        public boolean isOutOfOrder() {
            return outOfOrder;
        }
    }
}
