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

import static migratedb.core.api.TargetVersion.CURRENT;
import static migratedb.core.api.TargetVersion.LATEST;
import static migratedb.core.api.TargetVersion.NEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.ErrorDetails;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationState.Category;
import migratedb.core.api.MigrationType;
import migratedb.core.api.TargetVersion;
import migratedb.core.api.Version;
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
    private final TargetVersion target;
    private final MigrationPattern[] cherryPick;
    private final ValidationContext validationContext;
    /**
     * The migrations info calculated at the last refresh.
     */
    private List<MigrationInfo> migrationInfo;
    /**
     * Whether all of the specified schemas are empty or not.
     */
    private boolean allSchemasEmpty;

    /**
     * @param migrationResolver The migration resolver for available migrations.
     * @param schemaHistory     The schema history table for applied migrations.
     * @param configuration     The current configuration.
     * @param target            The target version up to which to retrieve the info.
     * @param cherryPick        The migrations to consider when migration.
     */
    public MigrationInfoServiceImpl(MigrationResolver migrationResolver,
                                    SchemaHistory schemaHistory,
                                    Database database,
                                    Configuration configuration,
                                    TargetVersion target,
                                    MigrationPattern[] cherryPick,
                                    ValidationContext validationContext) {
        this.migrationResolver = migrationResolver;
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.context = () -> configuration;
        this.database = database;
        this.target = target;
        this.cherryPick = cherryPick;
        this.validationContext = validationContext;
    }

    /**
     * Refreshes the info about migration state using the resolved migrations and schema history information.
     */
    public void refresh() {
        var refreshInfo = new RefreshInfo(
            migrationResolver.resolveMigrations(context),
            schemaHistory.allAppliedMigrations()
        );

        var versionContext = createVersionContext(refreshInfo);
        var newMigrationInfoList = new ArrayList<MigrationInfo>();
        newMigrationInfoList.addAll(infoForAlreadyAppliedMigrations(refreshInfo, versionContext));
        newMigrationInfoList.addAll(infoForPendingMigrations(refreshInfo, versionContext));
        newMigrationInfoList.addAll(infoForAlreadyAppliedRepeatableMigrations(refreshInfo, versionContext));
        newMigrationInfoList.addAll(infoForPendingRepeatableMigrations(refreshInfo, versionContext));

        newMigrationInfoList.sort(MigrationInfo.executionOrder());
        failOnMissingTarget(newMigrationInfoList);
        migrationInfo = newMigrationInfoList;
    }

    private VersionContext createVersionContext(RefreshInfo refreshInfo) {
        var context = new VersionContext.Builder(configuration.getIgnoreMigrationPatterns(), cherryPick);
        context.lastResolved = refreshInfo.latestResolved()
                                          .map(ResolvedMigration::getVersion)
                                          .orElse(context.lastResolved);
        context.latestBaselineMigration = refreshInfo.latestResolved(it -> it.getType().isBaselineMigration())
                                                     .map(ResolvedMigration::getVersion)
                                                     .orElse(context.latestBaselineMigration);
        context.latestRepeatableRuns = refreshInfo.latestRepeatableRuns();
        context.baseline = refreshInfo.latestApplied(it -> it.migration.getType() == MigrationType.BASELINE)
                                      .map(it -> it.migration.getVersion())
                                      .orElse(null);
        context.lastApplied = refreshInfo.latestApplied(it -> it.migration.getType() != MigrationType.DELETE &&
                                                              !it.deleted)
                                         .map(it -> it.migration.getVersion())
                                         .orElse(null);
        if (target == null) {
            context.target = null;
        } else {
            context.target = target.mapVersion(version -> version) // Easy case, it's already an actual version
                                   // Harder case, must be resolved to actual version
                                   .orElseGet(Map.of(
                                       CURRENT, () -> refreshInfo.currentApplied().map(it -> it.migration.getVersion()),
                                       LATEST, () -> refreshInfo.latestResolved().map(ResolvedMigration::getVersion),
                                       NEXT, () -> refreshInfo.nextMigration().map(ResolvedMigration::getVersion)
                                   ))
                                   .orElse(context.target);
        }
        return context.build();
    }

    private List<MigrationInfo> infoForPendingRepeatableMigrations(RefreshInfo refreshInfo,
                                                                   VersionContext versionContext) {
        return refreshInfo.pendingRepeatableMigrations()
                          .stream()
                          .map(it -> new MigrationInfoImpl(it,
                                                           null,
                                                           validationContext,
                                                           versionContext,
                                                           false,
                                                           false))
                          .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedRepeatableMigrations(RefreshInfo refreshInfo,
                                                                          VersionContext versionContext) {
        var result = new ArrayList<MigrationInfo>();
        for (var applied : refreshInfo.repeatableAppliedMigrations()) {
            var resolvedMigration = refreshInfo.findResolvedRepeatable(applied.migration).orElse(null);
            result.add(new MigrationInfoImpl(resolvedMigration,
                                             applied.migration,
                                             validationContext,
                                             versionContext,
                                             applied.deleted,
                                             false));
        }
        return result;
    }

    private void failOnMissingTarget(List<MigrationInfo> migrations) {
        if (configuration.getFailOnMissingTarget() && target != null) {
            target.withVersion(version -> {
                boolean targetFound = migrations.stream()
                                                .anyMatch(it -> version.equals(it.getVersion()));
                if (!targetFound) {
                    throw new MigrateDbException("No migration with a target version " + target +
                                                 " could be found. Ensure target is specified correctly and the " +
                                                 "migration" +
                                                 " exists.");
                }
            }).orElseDoNothing(); // Symbolic targets are never missing (I guess)
        }
    }

    private List<MigrationInfo> infoForPendingMigrations(RefreshInfo refreshInfo,
                                                         VersionContext versionContext) {
        return refreshInfo.pendingResolvedMigrations()
                          .stream()
                          .map(it -> new MigrationInfoImpl(it,
                                                           null,
                                                           validationContext,
                                                           versionContext,
                                                           false,
                                                           false))
                          .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedMigrations(RefreshInfo refreshInfo,
                                                                VersionContext versionContext) {
        var result = new ArrayList<MigrationInfo>();
        for (var applied : refreshInfo.versionedAppliedMigrations()) {
            var correspondingResolvedMigration = refreshInfo.findResolvedVersioned(applied.migration).orElse(null);
            result.add(new MigrationInfoImpl(correspondingResolvedMigration,
                                             applied.migration,
                                             validationContext,
                                             versionContext,
                                             applied.outOfOrder,
                                             applied.deleted));
        }
        return result;
    }

    private @Nullable ResolvedMigration findLatestBaselineMigration(Collection<ResolvedMigration> values) {
        return values.stream()
                     .filter(it -> it.getType().isBaselineMigration())
                     .max(Comparator.comparing(ResolvedMigration::getVersion))
                     .orElse(null);
    }

    @Override
    public MigrationInfo[] all() {
        return migrationInfo.toArray(MigrationInfo[]::new);
    }

    @Override
    public MigrationInfo current() {
        return migrationInfo.stream()
                            .filter(it -> it.getState().is(Category.APPLIED) &&
                                          it.getState() != MigrationState.DELETED &&
                                          it.getType() != MigrationType.DELETE &&
                                          it.getVersion() != null)
                            .max(Comparator.comparing(MigrationInfo::getVersion))
                            .or(this::latestRepeatableMigration)
                            .orElse(null);
    }

    private Optional<? extends MigrationInfo> latestRepeatableMigration() {
        for (int i = migrationInfo.size() - 1; i >= 0; i--) {
            MigrationInfo migrationInfo = this.migrationInfo.get(i);
            if (migrationInfo.getState().is(Category.APPLIED)
                && !MigrationState.DELETED.equals(migrationInfo.getState())
                && !MigrationType.DELETE.equals(migrationInfo.getType())
            ) {
                return Optional.of(migrationInfo);
            }
        }
        return Optional.empty();
    }

    @Override
    public MigrationInfo[] pending() {
        return filterByState(it -> it == MigrationState.PENDING);
    }

    @Override
    public MigrationInfo[] applied() {
        return filterByState(it -> it.is(Category.APPLIED));
    }

    @Override
    public MigrationInfo[] resolved() {
        return filterByState(it -> it.is(Category.RESOLVED));
    }

    @Override
    public MigrationInfo[] failed() {
        return filterByState(it -> it.is(Category.FAILED));
    }

    @Override
    public MigrationInfo[] future() {
        return filterByState(it -> it.is(Category.FUTURE));
    }

    private MigrationInfo[] filterByState(Predicate<MigrationState> predicate) {
        return migrationInfo.stream()
                            .filter(it -> predicate.test(it.getState()))
                            .toArray(MigrationInfo[]::new);
    }

    @Override
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

    /**
     * Treat all fields as read-only! (except when initializing them in RefreshInfo)
     */
    private static class AppliedMigrationAndAttributes {
        final AppliedMigration migration;
        boolean outOfOrder;
        boolean deleted;

        AppliedMigrationAndAttributes(AppliedMigration migration) {
            this.migration = migration;
        }
    }

    /**
     * Used to find the applied migrations that correspond to resolved migrations, the latest migration that satisfies
     * some conditions, etc. (this pulls a lot of state out of the refresh() method).
     * <p>
     * Has no observable mutable state.
     */
    private static final class RefreshInfo {
        private final Map<MigrationKey, ResolvedMigration> resolvedVersioned;
        private final List<AppliedMigrationAndAttributes> appliedInOrder;

        // Lazily initialized values
        private Map<String, Integer> latestRepeatableRuns;
        private Set<ResolvedMigration> nonAppliedResolvableMigrations;
        private Collection<ResolvedMigration> pendingRepeatableMigrations;
        private Map<String, ResolvedMigration> repeatableResolvedMigrations;
        private List<AppliedMigrationAndAttributes> repeatableAppliedMigrations;
        private List<AppliedMigrationAndAttributes> versionedAppliedMigrations;

        RefreshInfo(Collection<ResolvedMigration> resolvedMigrations,
                    Collection<AppliedMigration> appliedMigrations) {
            this.resolvedVersioned = resolvedMigrations.stream()
                                                       .filter(it -> !it.isRepeatable() &&
                                                                     !it.getType().isBaselineMigration())
                                                       .collect(Collectors.toMap(MigrationKey::new, it -> it));
            this.appliedInOrder = appliedMigrations.stream()
                                                   .map(AppliedMigrationAndAttributes::new)
                                                   .collect(Collectors.toList());
            setAppliedMigrationAttributes();
        }

        Optional<AppliedMigrationAndAttributes> currentApplied() {
            return appliedInOrder.stream()
                                 .filter(it -> it.migration.getType() != MigrationType.DELETE &&
                                               !it.deleted)
                                 .max(Comparator.comparing(it -> it.migration.getVersion()));
        }

        Optional<ResolvedMigration> findResolvedVersioned(AppliedMigration appliedMigration) {
            var found = resolvedVersioned.get(new MigrationKey(appliedMigration.getVersion(), false));
            if (found == null) {
                found = resolvedVersioned.get(new MigrationKey(appliedMigration.getVersion(), true));
            }
            return Optional.ofNullable(found);
        }

        Optional<ResolvedMigration> latestResolved(Predicate<ResolvedMigration> filter) {
            return resolvedVersioned.values().stream()
                                    .filter(filter)
                                    .max(Comparator.comparing(ResolvedMigration::getVersion));
        }

        Optional<ResolvedMigration> latestResolved() {
            return latestResolved(it -> true);
        }

        Optional<AppliedMigrationAndAttributes> latestApplied(Predicate<AppliedMigrationAndAttributes> filter) {
            return appliedInOrder.stream()
                                 .filter(filter)
                                 .max(Comparator.comparing(it -> it.migration.getVersion()));
        }

        Optional<AppliedMigrationAndAttributes> latestAppliedBaselineMigration() {
            return latestApplied(it -> it.migration.getType().isBaselineMigration() && it.migration.isSuccess());
        }

        Map<String, ResolvedMigration> repeatableResolvedMigrations() {
            return lazy(() -> repeatableResolvedMigrations,
                        it -> repeatableResolvedMigrations = it,
                        () -> resolvedVersioned.values()
                                               .stream()
                                               .filter(ResolvedMigration::isRepeatable)
                                               .collect(Collectors.toMap(ResolvedMigration::getDescription, it -> it)));
        }

        List<AppliedMigrationAndAttributes> repeatableAppliedMigrations() {
            return lazy(() -> repeatableAppliedMigrations,
                        it -> repeatableAppliedMigrations = it,
                        () -> appliedInOrder.stream()
                                            .filter(it -> it.migration.isRepeatable())
                                            .collect(Collectors.toList()));

        }

        List<AppliedMigrationAndAttributes> versionedAppliedMigrations() {
            return lazy(() -> versionedAppliedMigrations,
                        it -> versionedAppliedMigrations = it,
                        () -> appliedInOrder.stream()
                                            .filter(it -> !it.migration.isRepeatable())
                                            .collect(Collectors.toList()));
        }

        Collection<ResolvedMigration> nonAppliedResolvableMigrations() {
            return lazy(() -> nonAppliedResolvableMigrations,
                        it -> nonAppliedResolvableMigrations = it,
                        () -> {
                            // We REMOVE all that have been applied from this set
                            var result = new HashSet<>(resolvedVersioned.values());
                            for (var applied : versionedAppliedMigrations()) {
                                var resolved = findResolvedVersioned(applied.migration).orElse(
                                    null);
                                if (resolved != null &&
                                    applied.migration.getType() != MigrationType.DELETE &&
                                    !applied.deleted) {
                                    result.remove(resolved);
                                }
                            }
                            return result;
                        });
        }

        Collection<ResolvedMigration> pendingResolvedMigrations() {
            // The point of a baseline migration is to replace earlier migrations, so earlier migrations are NOT pending
            var latestBaselineMigration = latestAppliedBaselineMigration()
                .map(it -> it.migration)
                .orElse(null);
            if (latestBaselineMigration == null) {
                return nonAppliedResolvableMigrations();
            }
            return nonAppliedResolvableMigrations()
                .stream()
                .filter(it -> it.getVersion().compareTo(latestBaselineMigration.getVersion()) >= 0)
                .collect(Collectors.toList());
        }

        Collection<ResolvedMigration> pendingRepeatableMigrations() {
            return lazy(() -> pendingRepeatableMigrations,
                        it -> pendingRepeatableMigrations = it,
                        () -> {
                            // We REMOVE already applied migrations from this set
                            var latestRepeatableRuns = latestRepeatableRuns();
                            var repeatableResolved = repeatableResolvedMigrations();
                            var result = new HashSet<>(repeatableResolved.values());
                            for (var applied : repeatableAppliedMigrations()) {
                                String desc = applied.migration.getDescription();
                                int rank = applied.migration.getInstalledRank();

                                var resolvedMigration = repeatableResolved.get(desc);
                                int latestRank = latestRepeatableRuns.get(desc);

                                // If latest run is the same rank, it's not pending
                                if (!applied.deleted && applied.migration.getType() != MigrationType.DELETE
                                    && resolvedMigration != null
                                    && rank == latestRank &&
                                    resolvedMigration.checksumMatches(applied.migration.getChecksum())) {
                                    result.remove(resolvedMigration);
                                }
                            }
                            return result;
                        });
        }

        Optional<ResolvedMigration> nextMigration() {
            // Because baseline migrations supersede all previous versions, this isn't simply the first non-applied
            // version
            var nonApplied = new ArrayList<>(nonAppliedResolvableMigrations());
            if (nonApplied.isEmpty()) {
                return Optional.empty();
            }
            nonApplied.sort(Comparator.comparing(ResolvedMigration::getVersion));
            for (var i = nonApplied.size() - 1; i >= 0; i--) {
                var migration = nonApplied.get(i);
                if (migration.getType().isBaselineMigration()) {
                    return Optional.of(migration);
                }
            }
            return Optional.of(nonApplied.get(0));
        }

        Optional<ResolvedMigration> findResolvedRepeatable(AppliedMigration appliedMigration) {
            return Optional.ofNullable(repeatableResolvedMigrations().get(appliedMigration.getDescription()));
        }

        Map<String, Integer> latestRepeatableRuns() {
            return lazy(() -> latestRepeatableRuns, it -> latestRepeatableRuns = it, () -> {
                var result = new HashMap<String, Integer>();
                for (var applied : repeatableAppliedMigrations()) {
                    if (applied.deleted && applied.migration.getType() == MigrationType.DELETE) {
                        continue;
                    }

                    String desc = applied.migration.getDescription();
                    int rank = applied.migration.getInstalledRank();
                    var latestRank = result.get(desc);
                    if (latestRank != null || (rank > result.get(desc))) {
                        result.put(desc, rank);
                    }
                }
                return result;
            });
        }

        private void setAppliedMigrationAttributes() {
            // "Deleted" attribute
            for (var r : repeatableAppliedMigrations()) {
                if (r.migration.getType().equals(MigrationType.DELETE) && r.migration.isSuccess()) {
                    markRepeatableAsDeleted(r.migration.getDescription(), repeatableAppliedMigrations());
                }
            }
            for (var v : repeatableAppliedMigrations()) {
                if (v.migration.getType() == MigrationType.DELETE && v.migration.isSuccess()) {
                    markVersionedAsDeleted(v.migration.getVersion(), repeatableAppliedMigrations());
                }
            }
            // "Out of order" is anything that doesn't increment the version
            Version prev = null;
            for (var va : versionedAppliedMigrations()) {
                if (prev != null && prev.compareTo(va.migration.getVersion()) > 0) {
                    va.outOfOrder = true;
                }
                prev = va.migration.getVersion();
            }
        }

        /**
         * Marks the latest applied migration with this description as deleted.
         */
        private void markRepeatableAsDeleted(String description,
                                             List<AppliedMigrationAndAttributes> repatableApplied) {
            for (int i = repatableApplied.size() - 1; i >= 0; i--) {
                var applied = repatableApplied.get(i);
                if (!applied.migration.getType().isSynthetic() &&
                    description.equals(applied.migration.getDescription())) {
                    applied.deleted = true;
                    return;
                }
            }
        }

        /**
         * Marks the latest applied migration with this version as deleted.
         */
        private void markVersionedAsDeleted(Version version,
                                            List<AppliedMigrationAndAttributes> versionedApplied) {
            for (int i = versionedApplied.size() - 1; i >= 0; i--) {
                var applied = versionedApplied.get(i);
                if (!applied.migration.getType().isSynthetic() && version.equals(applied.migration.getVersion())) {
                    if (applied.deleted) {
                        throw new MigrateDbException(
                            "Corrupted schema history: multiple delete entries for version " + version,
                            ErrorCode.DUPLICATE_DELETED_MIGRATION);
                    } else {
                        applied.deleted = true;
                        return;
                    }
                }
            }
        }

        private static <T> T lazy(Supplier<T> getter, Consumer<T> setter, Supplier<T> computation) {
            var result = getter.get();
            if (result == null) {
                result = computation.get();
                setter.accept(result);
            }
            return result;
        }

        private static final class MigrationKey {
            final @Nullable Version version;
            final boolean isBaselineMigration;

            MigrationKey(ResolvedMigration m) {
                this(m.getVersion(), m.getType().isBaselineMigration());
            }

            MigrationKey(@Nullable Version version, boolean isBaselineMigration) {
                this.version = version;
                this.isBaselineMigration = isBaselineMigration;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof MigrationKey)) {
                    return false;
                }
                MigrationKey other = (MigrationKey) o;
                return isBaselineMigration == other.isBaselineMigration &&
                       Objects.equals(version, other.version);
            }

            @Override
            public int hashCode() {
                return Objects.hash(version, isBaselineMigration);
            }
        }
    }
}
