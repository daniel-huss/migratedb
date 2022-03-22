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
package migratedb.core.internal.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.ErrorDetails;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationType;
import migratedb.core.api.MigrationVersion;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.api.output.InfoResult;
import migratedb.core.api.output.OperationResult;
import migratedb.core.api.output.ValidateOutput;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.schemahistory.SchemaHistory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MigrationInfoServiceImpl extends OperationResult implements MigrationInfoService {
    private final MigrationResolver migrationResolver;
    private final Configuration configuration;
    private final Database database;
    private final Context context;
    private final SchemaHistory schemaHistory;
    private final MigrationVersion target;
    private final MigrationPattern[] cherryPick;
    private final boolean outOfOrder;
    private final boolean pending;
    private final boolean missing;
    private final boolean ignored;
    private final boolean future;
    /**
     * The migrations info calculated at the last refresh.
     */
    private List<MigrationInfo> migrationInfo;
    /**
     * Whether all of the specified schemas are empty or not.
     */
    private Boolean allSchemasEmpty;

    /**
     * @param migrationResolver The migration resolver for available migrations.
     * @param schemaHistory     The schema history table for applied migrations.
     * @param configuration     The current configuration.
     * @param target            The target version up to which to retrieve the info.
     * @param outOfOrder        Allows migrations to be run "out of order".
     * @param cherryPick        The migrations to consider when migration.
     * @param pending           Whether pending migrations are allowed.
     * @param missing           Whether missing migrations are allowed.
     * @param ignored           Whether ignored migrations are allowed.
     * @param future            Whether future migrations are allowed.
     */
    public MigrationInfoServiceImpl(MigrationResolver migrationResolver,
                                    SchemaHistory schemaHistory, Database database, Configuration configuration,
                                    MigrationVersion target, boolean outOfOrder, MigrationPattern[] cherryPick,
                                    boolean pending, boolean missing, boolean ignored, boolean future) {
        this.migrationResolver = migrationResolver;
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.context = () -> configuration;
        this.database = database;
        this.target = target;
        this.outOfOrder = outOfOrder;
        this.cherryPick = cherryPick;
        this.pending = pending;
        this.missing = missing;
        this.ignored = ignored || cherryPick != null;
        this.future = future;
    }

    private static final class VersionedKey {
        public final MigrationVersion version;
        public final boolean isUndo;

        private VersionedKey(MigrationVersion version, boolean isUndo) {
            this.version = version;
            this.isUndo = isUndo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, isUndo);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof VersionedKey)) {
                return false;
            }
            var other = (VersionedKey) obj;
            return Objects.equals(version, other.version) &&
                   Objects.equals(isUndo, other.isUndo);
        }
    }

    private static final class AppliedMigrationWithAttribs {
        public final AppliedMigration migration;
        public final AppliedMigrationAttributes attribs = new AppliedMigrationAttributes();

        private AppliedMigrationWithAttribs(AppliedMigration migration) {
            this.migration = migration;
        }
    }

    /**
     * Refreshes the info about migration state using the resolved migrations and schema history information.
     */
    public void refresh() {
        var resolvedMigrations = migrationResolver.resolveMigrations(context);
        var appliedMigrationsInOrder = schemaHistory.allAppliedMigrations();

        var context = new MigrationInfoContext();
        context.outOfOrder = outOfOrder;
        context.pending = pending;
        context.missing = missing;
        context.ignored = ignored;
        context.future = future;
        context.ignorePatterns = configuration.getIgnoreMigrationPatterns();
        context.target = target;
        context.cherryPick = cherryPick;

        // Group _resolved_ migrations into versioned and repeatable
        var versioned = onlyVersionedMigrations(resolvedMigrations);
        var repeatable = onlyRepeatableMigrations(resolvedMigrations);
        updateStateFromVersionedResolvedMigrations(context, versioned);

        // Group _applied_ migrations into versioned and repeatable
        var versionedApplied = onlyVersionedMigrations(appliedMigrationsInOrder);
        var appliedBaselineMigration = findAppliedBaselineMigration(versionedApplied);
        var repeatableApplied = onlyRepeatableMigrations(appliedMigrationsInOrder);
        updateLatestRepeatableRuns(context, repeatableApplied);

        // TODO
        //
        // There's a large code block missing here, probably something that deals with undo migrations.
        //
        //

        updateStateFromSyntheticMigrations(context, versionedApplied);
        updateStateFromSyntheticMigrations(repeatableApplied);
        updateOutOfOrder(versionedApplied);
        updateLastApplied(context, versionedApplied);

        // TODO
        // Code block missing here, probably filtering that implements cherry-picking?
        //
        //

        var notAppliedMigrations = onlyNotAppliedMigrations(versioned, versionedApplied);
        var pendingMigrations = onlyPendingMigrations(notAppliedMigrations, context.latestBaselineMigration);
        var pendingRepeatableMigrations = onlyPendingRepeatableMigrations(repeatable, repeatableApplied, context);

        // Time to build the new migration info list
        var newMigrationInfoList = new ArrayList<MigrationInfo>();
        newMigrationInfoList.addAll(infoForAlreadyAppliedMigrations(versionedApplied, versioned, context));
        newMigrationInfoList.addAll(infoForPendingMigrations(pendingMigrations, context));
        newMigrationInfoList.addAll(infoForAlreadyAppliedRepeatableMigrations(repeatableApplied, repeatable, context));
        newMigrationInfoList.addAll(infoForPendingRepeatableMigrations(pendingRepeatableMigrations, context));

        failOnMissingTarget(newMigrationInfoList);
        newMigrationInfoList.sort(Comparator.naturalOrder());
        migrationInfo = newMigrationInfoList;

        // Now one might wonder why we mutate the context object here, aren't we done building the new migrationInfo?
        //
        // Well, if you look closely, you'll find that we've passed our mutable context object to MigrationInfoImpl,
        // and our modifications will actually change its bloody behavior. It's good to know that pending() somehow
        // seems to work even though we haven't finished setting up the context object.
        //
        // ... GENIUS!
        //
        // TODO MigrationInfo should be a dumb, immutable data structure. The computations should be done here, once, in
        // SMALLLLLLL methods and with additional data structures if needed.

        if (target == MigrationVersion.NEXT) {
            MigrationInfo[] pending = pending();
            if (pending.length == 0) {
                context.target = null;
            } else {
                context.target = pending[0].getVersion();
            }
        } else if (MigrationVersion.CURRENT == target) {
            context.target = context.lastApplied;
        }
    }

    private List<MigrationInfo> infoForPendingRepeatableMigrations(Set<ResolvedMigration> pendingRepeatableMigrations,
                                                                   MigrationInfoContext context) {
        return pendingRepeatableMigrations.stream()
                                          .map(it -> new MigrationInfoImpl(it,
                                                                           null,
                                                                           context,
                                                                           false,
                                                                           false,
                                                                           false))
                                          .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedRepeatableMigrations(
        List<AppliedMigrationWithAttribs> repeatableApplied,
        Map<String, ResolvedMigration> repeatable,
        MigrationInfoContext context) {
        var result = new ArrayList<MigrationInfo>();
        for (var a : repeatableApplied) {
            var resolvedMigration = repeatable.get(a.migration.getDescription());
            result.add(new MigrationInfoImpl(resolvedMigration,
                                             a.migration,
                                             context,
                                             false,
                                             a.attribs.deleted,
                                             false));
        }
        return result;
    }

    private Set<ResolvedMigration> onlyPendingRepeatableMigrations(Map<String, ResolvedMigration> repeatable,
                                                                   List<AppliedMigrationWithAttribs> repeatableApplied,
                                                                   MigrationInfoContext context) {
        var result = new HashSet<>(repeatable.values()); // We REMOVE already applied migrations from this set
        for (var av : repeatableApplied) {
            String desc = av.migration.getDescription();
            int rank = av.migration.getInstalledRank();

            ResolvedMigration resolvedMigration = repeatable.get(desc);
            int latestRank = context.latestRepeatableRuns.get(desc);

            // If latest run is the same rank, it's not pending
            if (!av.attribs.deleted && av.migration.getType() != MigrationType.DELETE
                && resolvedMigration != null
                && rank == latestRank &&
                resolvedMigration.checksumMatches(av.migration.getChecksum())) {
                result.remove(resolvedMigration);
            }
        }
        return result;
    }

    private void updateLatestRepeatableRuns(MigrationInfoContext context,
                                            List<AppliedMigrationWithAttribs> repeatableApplied) {
        for (var a : repeatableApplied) {
            if (a.attribs.deleted && a.migration.getType() == MigrationType.DELETE) {
                continue;
            }
            AppliedMigration appliedRepeatableMigration = a.migration;

            String desc = appliedRepeatableMigration.getDescription();
            int rank = appliedRepeatableMigration.getInstalledRank();
            Map<String, Integer> latestRepeatableRuns = context.latestRepeatableRuns;

            if (!latestRepeatableRuns.containsKey(desc) || (rank > latestRepeatableRuns.get(desc))) {
                latestRepeatableRuns.put(desc, rank);
            }
        }
    }

    private void failOnMissingTarget(ArrayList<MigrationInfo> newMigrationInfoList) {
        if (configuration.getFailOnMissingTarget() &&
            target != null &&
            target != MigrationVersion.CURRENT &&
            target != MigrationVersion.LATEST &&
            target != MigrationVersion.NEXT) {
            boolean targetFound = false;

            for (MigrationInfo migration : newMigrationInfoList) {
                if (target.compareTo(migration.getVersion()) == 0) {
                    targetFound = true;
                    break;
                }
            }

            if (!targetFound) {
                throw new MigrateDbException("No migration with a target version " + target +
                                             " could be found. Ensure target is specified correctly and the migration" +
                                             " exists.");
            }
        }
    }

    private List<MigrationInfo> infoForPendingMigrations(List<ResolvedMigration> pendingMigrations,
                                                         MigrationInfoContext context) {
        return pendingMigrations.stream()
                                .map(it -> new MigrationInfoImpl(it,
                                                                 null,
                                                                 context,
                                                                 false,
                                                                 false,
                                                                 false))
                                .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedMigrations(List<AppliedMigrationWithAttribs> versionedApplied,
                                                                Map<VersionedKey, ResolvedMigration> versioned,
                                                                MigrationInfoContext context) {
        var result = new ArrayList<MigrationInfo>();
        for (var a : versionedApplied) {
            var searchKey = new VersionedKey(a.migration.getVersion(), a.migration.getType().isUndo());
            var correspondingResolvedMigration = versioned.get(searchKey);
            result.add(new MigrationInfoImpl(correspondingResolvedMigration,
                                             a.migration,
                                             context,
                                             a.attribs.outOfOrder,
                                             a.attribs.deleted,
                                             a.attribs.undone));
        }
        return result;
    }

    private List<ResolvedMigration> onlyPendingMigrations(Set<ResolvedMigration> notAppliedMigrations,
                                                          MigrationVersion latestBaselineMigration) {
        // The point of a baseline migration is to replace earlier migrations, so earlier migrations are NOT pending
        return notAppliedMigrations.stream()
                                   .filter(it -> it.getVersion().compareTo(latestBaselineMigration) >= 0)
                                   .collect(Collectors.toList());
    }

    private Set<ResolvedMigration> onlyNotAppliedMigrations(Map<VersionedKey, ResolvedMigration> versioned,
                                                            List<AppliedMigrationWithAttribs> versionedApplied) {
        var result = new HashSet<>(versioned.values()); // We REMOVE all that have been applied from this set
        for (var a : versionedApplied) {
            var resolvedMigrationThatHasBeenApplied = versioned.get(
                new VersionedKey(a.migration.getVersion(), a.migration.getType().isUndo())
            );
            if (resolvedMigrationThatHasBeenApplied != null && a.migration.getType() != MigrationType.DELETE &&
                !a.attribs.deleted) {
                result.remove(resolvedMigrationThatHasBeenApplied);
            }
        }
        return result;
    }

    private void updateLastApplied(MigrationInfoContext context,
                                   List<AppliedMigrationWithAttribs> versionedApplied) {
        context.lastApplied = versionedApplied.stream()
                                              .filter(it -> it.migration.getType() != MigrationType.DELETE &&
                                                            !it.attribs.deleted)
                                              .max(Comparator.comparing(it -> it.migration.getVersion()))
                                              .map(it -> it.migration.getVersion())
                                              .orElse(null);
    }

    private void updateOutOfOrder(List<AppliedMigrationWithAttribs> versionedApplied) {
        // Out of order is anything that doesn't increment the version
        MigrationVersion prev = null;
        for (var va : versionedApplied) {
            if (prev != null && prev.compareTo(va.migration.getVersion()) > 0) {
                va.attribs.outOfOrder = true;
            }
            prev = va.migration.getVersion();
        }
    }

    private @Nullable AppliedMigration findAppliedBaselineMigration(
        List<AppliedMigrationWithAttribs> versionedApplied) {
        return versionedApplied.stream()
                               .filter(it -> it.migration.getType().isBaselineMigration() && it.migration.isSuccess())
                               .max(Comparator.comparing(it -> it.migration.getVersion()))
                               .map(it -> it.migration)
                               .orElse(null);
    }

    private @Nullable ResolvedMigration findLatestBaselineMigration(Collection<ResolvedMigration> values) {
        return values.stream()
                     .filter(it -> it.getType().isBaselineMigration())
                     .max(Comparator.comparing(ResolvedMigration::getVersion))
                     .orElse(null);
    }

    private void updateStateFromVersionedResolvedMigrations(MigrationInfoContext context,
                                                            Map<VersionedKey, ResolvedMigration> versioned) {
        for (var migration : versioned.values()) {
            if (migration.getVersion().compareTo(context.lastResolved) > 0) {
                context.lastResolved = migration.getVersion();
            }
            if (migration.getType().isBaselineMigration() &&
                migration.getVersion().compareTo(context.latestBaselineMigration) > 0) {
                context.latestBaselineMigration = migration.getVersion();
            }
        }
    }

    private Map<String, ResolvedMigration> onlyRepeatableMigrations(Collection<ResolvedMigration> resolvedMigrations) {
        return resolvedMigrations.stream()
                                 .filter(ResolvedMigration::isRepeatable)
                                 .collect(Collectors.toMap(ResolvedMigration::getDescription, it -> it));
    }

    private Map<VersionedKey, ResolvedMigration> onlyVersionedMigrations(
        Collection<ResolvedMigration> resolvedMigrations) {
        return resolvedMigrations.stream()
                                 .filter(it -> !it.isRepeatable())
                                 .collect(Collectors.toMap(it -> new VersionedKey(it.getVersion(),
                                                                                  it.getType().isUndo()),
                                                           it -> it));
    }

    private List<AppliedMigrationWithAttribs> onlyRepeatableMigrations(List<AppliedMigration> appliedMigrations) {
        return appliedMigrations.stream()
                                .filter(AppliedMigration::isRepeatable)
                                .map(AppliedMigrationWithAttribs::new)
                                .collect(Collectors.toList());
    }

    private List<AppliedMigrationWithAttribs> onlyVersionedMigrations(List<AppliedMigration> appliedMigrations) {
        return appliedMigrations.stream()
                                .filter(it -> !it.isRepeatable())
                                .map(AppliedMigrationWithAttribs::new)
                                .collect(Collectors.toList());
    }

    private void updateStateFromSyntheticMigrations(List<AppliedMigrationWithAttribs> repeatableApplied) {
        for (var r : repeatableApplied) {
            if (r.migration.getType().equals(MigrationType.DELETE) && r.migration.isSuccess()) {
                markRepeatableAsDeleted(r.migration.getDescription(), repeatableApplied);
            }
        }
    }

    private void updateStateFromSyntheticMigrations(MigrationInfoContext context,
                                                    List<AppliedMigrationWithAttribs> versionedApplied) {
        for (var v : versionedApplied) {
            if (v.migration.getType() == MigrationType.SCHEMA) {
                context.schema = v.migration.getVersion();
            }
            if (v.migration.getType() == MigrationType.BASELINE) {
                context.baseline = v.migration.getVersion();
            }
            if (v.migration.getType() == MigrationType.DELETE && v.migration.isSuccess()) {
                markVersionedAsDeleted(v.migration.getVersion(), versionedApplied);
            }
        }
    }

    /**
     * Marks the latest applied migration with this description as deleted.
     *
     * @param description       The description to match
     * @param appliedRepeatable The discovered applied migrations
     */
    private void markRepeatableAsDeleted(String description,
                                         List<AppliedMigrationWithAttribs> appliedRepeatable) {
        for (int i = appliedRepeatable.size() - 1; i >= 0; i--) {
            var ar = appliedRepeatable.get(i);
            if (!ar.migration.getType().isSynthetic() && description.equals(ar.migration.getDescription())) {
                ar.attribs.deleted = true;
                return;
            }
        }
    }

    /**
     * Marks the latest applied migration with this version as deleted.
     *
     * @param version          The version.
     * @param appliedVersioned The applied migrations.
     */
    private void markVersionedAsDeleted(MigrationVersion version,
                                        List<AppliedMigrationWithAttribs> appliedVersioned) {
        for (int i = appliedVersioned.size() - 1; i >= 0; i--) {
            var v = appliedVersioned.get(i);
            if (!v.migration.getType().isSynthetic() && version.equals(v.migration.getVersion())) {
                if (v.attribs.deleted) {
                    throw new MigrateDbException(
                        "Corrupted schema history: multiple delete entries for version " + version,
                        ErrorCode.DUPLICATE_DELETED_MIGRATION);
                } else {
                    v.attribs.deleted = true;
                    return;
                }
            }
        }
    }

    @Override
    public MigrationInfo[] all() {
        return migrationInfo.toArray(MigrationInfo[]::new);
    }

    @Override
    public MigrationInfo current() {
        MigrationInfo current = null;
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (migrationInfo.getState().isApplied()
                && !MigrationState.DELETED.equals(migrationInfo.getState())
                && !migrationInfo.getType().equals(MigrationType.DELETE)

                && migrationInfo.getVersion() != null
                && (current == null || migrationInfo.getVersion().compareTo(current.getVersion()) > 0)) {
                current = migrationInfo;
            }
        }
        if (current != null) {
            return current;
        }

        // If no versioned migration has been applied so far, fall back to the latest repeatable one
        for (int i = migrationInfo.size() - 1; i >= 0; i--) {
            MigrationInfo migrationInfo = this.migrationInfo.get(i);
            if (migrationInfo.getState().isApplied()
                && !MigrationState.DELETED.equals(migrationInfo.getState())
                && !MigrationType.DELETE.equals(migrationInfo.getType())

            ) {
                return migrationInfo;
            }
        }

        return null;
    }

    @Override
    public MigrationInfo[] pending() {
        List<MigrationInfo> pendingMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (MigrationState.PENDING == migrationInfo.getState()) {
                pendingMigrations.add(migrationInfo);
            }
        }

        return pendingMigrations.toArray(new MigrationInfo[0]);
    }

    @Override
    public MigrationInfo[] applied() {
        List<MigrationInfo> appliedMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (migrationInfo.getState().isApplied()) {
                appliedMigrations.add(migrationInfo);
            }
        }

        return appliedMigrations.toArray(new MigrationInfo[0]);
    }

    /**
     * @return The resolved migrations. An empty array if none.
     */
    public MigrationInfo[] resolved() {
        List<MigrationInfo> resolvedMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (migrationInfo.getState().isResolved()) {
                resolvedMigrations.add(migrationInfo);
            }
        }

        return resolvedMigrations.toArray(new MigrationInfo[0]);
    }

    /**
     * @return The failed migrations. An empty array if none.
     */
    public MigrationInfo[] failed() {
        List<MigrationInfo> failedMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (migrationInfo.getState().isFailed()) {
                failedMigrations.add(migrationInfo);
            }
        }

        return failedMigrations.toArray(new MigrationInfo[0]);
    }

    /**
     * @return The future migrations. An empty array if none.
     */
    public MigrationInfo[] future() {
        List<MigrationInfo> futureMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (((migrationInfo.getState() == MigrationState.FUTURE_SUCCESS)
                 || (migrationInfo.getState() == MigrationState.FUTURE_FAILED))

            ) {
                futureMigrations.add(migrationInfo);
            }
        }

        return futureMigrations.toArray(new MigrationInfo[0]);
    }

    /**
     * @return The out of order migrations. An empty array if none.
     */
    public MigrationInfo[] outOfOrder() {
        List<MigrationInfo> outOfOrderMigrations = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfo) {
            if (migrationInfo.getState() == MigrationState.OUT_OF_ORDER) {
                outOfOrderMigrations.add(migrationInfo);
            }
        }

        return outOfOrderMigrations.toArray(new MigrationInfo[0]);
    }

    /**
     * @return The list of migrations that failed validation, which is empty if everything is fine.
     */
    public List<ValidateOutput> validate() {
        List<ValidateOutput> invalidMigrations = new ArrayList<>();

        for (MigrationInfo migrationInfo : migrationInfo) {
            ErrorDetails validateError = migrationInfo.validate();
            if (validateError != null) {
                invalidMigrations.add(CommandResultFactory.createValidateOutput(migrationInfo, validateError));
            }
        }
        return invalidMigrations;
    }

    public void setAllSchemasEmpty(Schema[] schemas) {
        allSchemasEmpty = Arrays.stream(schemas).filter(Schema::exists).allMatch(Schema::empty);
    }

    @Override
    public InfoResult getInfoResult() {
        return getInfoResult(all());
    }

    public InfoResult getInfoResult(MigrationInfo[] infos) {
        return CommandResultFactory.createInfoResult(context.getConfiguration(),
                                                     database,
                                                     infos,
                                                     current(),
                                                     allSchemasEmpty);
    }

}
