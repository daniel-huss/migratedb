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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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

        var versionContextBuilder = new VersionContext.Builder(configuration.getIgnoreMigrationPatterns(), cherryPick);

        // Group _resolved_ migrations into versioned and repeatable
        var versioned = onlyVersionedMigrations(resolvedMigrations);
        var repeatable = onlyRepeatableMigrations(resolvedMigrations);
        updateStateFromVersionedResolvedMigrations(versionContextBuilder, versioned);

        // Group _applied_ migrations into versioned and repeatable
        var versionedApplied = onlyVersionedMigrations(appliedMigrationsInOrder);
        var appliedBaselineMigration = findAppliedBaselineMigration(versionedApplied);
        var repeatableApplied = onlyRepeatableMigrations(appliedMigrationsInOrder);

        // Mutate various state in version context builder and applied migration attributes -_-
        updateLatestRepeatableRuns(versionContextBuilder, repeatableApplied);
        updateStateFromSyntheticMigrations(versionContextBuilder, versionedApplied);
        updateStateFromSyntheticMigrations(repeatableApplied);
        updateOutOfOrderAttribute(versionedApplied);
        updateLastApplied(versionContextBuilder, versionedApplied);
        updateTargetVersion(versionContextBuilder, versionedApplied, versioned);

        var versionContext = versionContextBuilder.build();
        var nonAppliedMigrations = onlyNonAppliedMigrations(versioned, versionedApplied);
        var pendingMigrations = onlyPendingMigrations(nonAppliedMigrations,
                                                      versionContextBuilder.latestBaselineMigration);
        var pendingRepeatableMigrations = onlyPendingRepeatableMigrations(repeatable,
                                                                          repeatableApplied,
                                                                          versionContext);

        // Time to build the new migration info list
        var newMigrationInfoList = new ArrayList<MigrationInfo>();
        newMigrationInfoList.addAll(infoForAlreadyAppliedMigrations(versionedApplied, versioned, versionContext));
        newMigrationInfoList.addAll(infoForPendingMigrations(pendingMigrations, versionContext));
        newMigrationInfoList.addAll(infoForAlreadyAppliedRepeatableMigrations(repeatableApplied,
                                                                              repeatable,
                                                                              versionContext));
        newMigrationInfoList.addAll(infoForPendingRepeatableMigrations(pendingRepeatableMigrations, versionContext));

        newMigrationInfoList.sort(MigrationInfo.executionOrder());
        failOnMissingTarget(newMigrationInfoList);
        migrationInfo = newMigrationInfoList;
    }

    private void updateTargetVersion(VersionContext.Builder context,
                                     List<AppliedMigrationWithAttribs> versionedApplied,
                                     Map<Version, ResolvedMigration> versioned) {
        context.target = target.mapVersion(version -> version)
                               .orElseGet(Map.of(
                                   TargetVersion.CURRENT, () -> resolveCurrentVersion(versionedApplied),
                                   TargetVersion.LATEST, () -> resolveLatestVersion(versioned),
                                   TargetVersion.NEXT, () -> resolveNextVersion(versioned, versionedApplied)
                               ));
    }

    private @Nullable Version resolveNextVersion(Map<Version, ResolvedMigration> versioned,
                                                 List<AppliedMigrationWithAttribs> versionedApplied) {
        // Because baseline migrations supersede all previous versions, this isn't simply the first non-applied version
        var nonApplied = new ArrayList<>(onlyNonAppliedMigrations(versioned, versionedApplied));
        if (nonApplied.isEmpty()) {
            return null;
        }
        nonApplied.sort(Comparator.comparing(ResolvedMigration::getVersion));
        for (var i = nonApplied.size() - 1; i >= 0; i--) {
            var migration = nonApplied.get(i);
            if (migration.getType().isBaselineMigration()) {
                return migration.getVersion();
            }
        }
        return nonApplied.get(0).getVersion();
    }

    private @Nullable Version resolveLatestVersion(Map<Version, ResolvedMigration> versioned) {
        return versioned.keySet()
                        .stream()
                        .max(Comparator.naturalOrder())
                        .orElse(null);
    }

    private @Nullable Version resolveCurrentVersion(List<AppliedMigrationWithAttribs> versionedApplied) {
        // The highest applied version that is not deleted
        return versionedApplied.stream()
                               .filter(it -> it.migration.getType() != MigrationType.DELETE &&
                                             !it.attribs.deleted)
                               .map(it -> it.migration.getVersion())
                               .max(Comparator.naturalOrder())
                               .orElse(null);
    }

    private List<MigrationInfo> infoForPendingRepeatableMigrations(Set<ResolvedMigration> pendingRepeatableMigrations,
                                                                   VersionContext versionContext) {
        return pendingRepeatableMigrations.stream()
                                          .map(it -> new MigrationInfoImpl(it,
                                                                           null,
                                                                           validationContext,
                                                                           versionContext,
                                                                           false,
                                                                           false))
                                          .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedRepeatableMigrations(
        List<AppliedMigrationWithAttribs> repeatableApplied,
        Map<String, ResolvedMigration> repeatable,
        VersionContext versionContext) {
        var result = new ArrayList<MigrationInfo>();
        for (var a : repeatableApplied) {
            var resolvedMigration = repeatable.get(a.migration.getDescription());
            result.add(new MigrationInfoImpl(resolvedMigration,
                                             a.migration,
                                             validationContext,
                                             versionContext,
                                             a.attribs.deleted,
                                             false));
        }
        return result;
    }

    private Set<ResolvedMigration> onlyPendingRepeatableMigrations(Map<String, ResolvedMigration> repeatable,
                                                                   List<AppliedMigrationWithAttribs> repeatableApplied,
                                                                   VersionContext context) {
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

    private void updateLatestRepeatableRuns(VersionContext.Builder versionContext,
                                            List<AppliedMigrationWithAttribs> repeatableApplied) {
        for (var a : repeatableApplied) {
            if (a.attribs.deleted && a.migration.getType() == MigrationType.DELETE) {
                continue;
            }
            AppliedMigration appliedRepeatableMigration = a.migration;

            String desc = appliedRepeatableMigration.getDescription();
            int rank = appliedRepeatableMigration.getInstalledRank();
            Map<String, Integer> latestRepeatableRuns = versionContext.latestRepeatableRuns;

            if (!latestRepeatableRuns.containsKey(desc) || (rank > latestRepeatableRuns.get(desc))) {
                latestRepeatableRuns.put(desc, rank);
            }
        }
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

    private List<MigrationInfo> infoForPendingMigrations(List<ResolvedMigration> pendingMigrations,
                                                         VersionContext versionContext) {
        return pendingMigrations.stream()
                                .map(it -> new MigrationInfoImpl(it,
                                                                 null,
                                                                 validationContext,
                                                                 versionContext,
                                                                 false,
                                                                 false))
                                .collect(Collectors.toList());
    }

    private List<MigrationInfo> infoForAlreadyAppliedMigrations(List<AppliedMigrationWithAttribs> versionedApplied,
                                                                Map<Version, ResolvedMigration> versioned,
                                                                VersionContext versionContext) {
        var result = new ArrayList<MigrationInfo>();
        for (var a : versionedApplied) {
            var correspondingResolvedMigration = versioned.get(a.migration.getVersion());
            result.add(new MigrationInfoImpl(correspondingResolvedMigration,
                                             a.migration,
                                             validationContext,
                                             versionContext,
                                             a.attribs.outOfOrder,
                                             a.attribs.deleted));
        }
        return result;
    }

    private List<ResolvedMigration> onlyPendingMigrations(Set<ResolvedMigration> notAppliedMigrations,
                                                          Version latestBaselineMigration) {
        // The point of a baseline migration is to replace earlier migrations, so earlier migrations are NOT pending
        return notAppliedMigrations.stream()
                                   .filter(it -> it.getVersion().compareTo(latestBaselineMigration) >= 0)
                                   .collect(Collectors.toList());
    }

    private Set<ResolvedMigration> onlyNonAppliedMigrations(Map<Version, ResolvedMigration> versioned,
                                                            List<AppliedMigrationWithAttribs> versionedApplied) {
        var result = new HashSet<>(versioned.values()); // We REMOVE all that have been applied from this set
        for (var a : versionedApplied) {
            var resolvedMigrationThatHasBeenApplied = versioned.get(a.migration.getVersion());
            if (resolvedMigrationThatHasBeenApplied != null && a.migration.getType() != MigrationType.DELETE &&
                !a.attribs.deleted) {
                result.remove(resolvedMigrationThatHasBeenApplied);
            }
        }
        return result;
    }

    private void updateLastApplied(VersionContext.Builder context,
                                   List<AppliedMigrationWithAttribs> versionedApplied) {
        context.lastApplied = versionedApplied.stream()
                                              .filter(it -> it.migration.getType() != MigrationType.DELETE &&
                                                            !it.attribs.deleted)
                                              .max(Comparator.comparing(it -> it.migration.getVersion()))
                                              .map(it -> it.migration.getVersion())
                                              .orElse(null);
    }

    private void updateOutOfOrderAttribute(List<AppliedMigrationWithAttribs> versionedApplied) {
        // Out of order is anything that doesn't increment the version
        Version prev = null;
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

    private void updateStateFromVersionedResolvedMigrations(VersionContext.Builder context,
                                                            Map<Version, ResolvedMigration> versioned) {
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

    private Map<Version, ResolvedMigration> onlyVersionedMigrations(
        Collection<ResolvedMigration> resolvedMigrations) {
        return resolvedMigrations.stream()
                                 .filter(it -> !it.isRepeatable())
                                 .collect(Collectors.toMap(ResolvedMigration::getVersion, it -> it));
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

    private void updateStateFromSyntheticMigrations(VersionContext.Builder context,
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
    private void markVersionedAsDeleted(Version version,
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

}
