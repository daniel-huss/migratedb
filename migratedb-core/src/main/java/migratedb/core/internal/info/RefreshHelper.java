/*
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

import static java.util.stream.Collectors.toMap;
import static migratedb.core.api.TargetVersion.CURRENT;
import static migratedb.core.api.TargetVersion.LATEST;
import static migratedb.core.api.TargetVersion.NEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationType;
import migratedb.core.api.TargetVersion;
import migratedb.core.api.Version;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.info.NavigableMigrations.RepeatableMigrationEntry;
import migratedb.core.internal.info.NavigableMigrations.VersionedMigrationEntry;
import org.checkerframework.checker.nullness.qual.Nullable;

final class RefreshHelper {
    private final @Nullable MigrationPattern[] cherryPick;
    private final TargetVersion target;
    private final ValidationContext validationContext;
    private final NavigableMigrations migrations;

    RefreshHelper(Collection<ResolvedMigration> resolvedMigrations,
                  Collection<AppliedMigration> appliedMigrations,
                  @Nullable MigrationPattern[] cherryPick,
                  TargetVersion target,
                  ValidationContext validationContext) {
        this.cherryPick = cherryPick;
        this.target = target;
        this.validationContext = validationContext;
        this.migrations = new NavigableMigrationsBuilder(resolvedMigrations, appliedMigrations).build();
    }

    List<MigrationInfo> getMigrationInfo() {
        var currentApplied = migrations.latestAppliedVersioned().orElse(null);
        var latestResolved = migrations.latestResolvedVersioned().orElse(null);
        var nextResolved = resolvedIncrementalAfter(currentApplied).orElse(null);
        var resolvedTarget = resolveTarget(currentApplied, latestResolved, nextResolved).orElse(null);
        var pendingBaselineMigration = findPendingBaselineMigration(currentApplied, resolvedTarget).orElse(null);
        var maxResolvedVersion = findMaxResolvedVersion(pendingBaselineMigration).orElse(null);

        var versionedMigrationsWithState = computeStateOfVersionedMigrations(resolvedTarget,
                                                                             pendingBaselineMigration,
                                                                             maxResolvedVersion);
        var repeatableMigrationsWithState = computeStateOfRepeatableMigrations();

        return createSortedMigrationInfoList(pendingBaselineMigration,
                                             versionedMigrationsWithState,
                                             repeatableMigrationsWithState);
    }

    private Optional<Version> findMaxResolvedVersion(@Nullable ResolvedMigration pendingBaselineMigration) {
        return migrations
            .versionedMigrations
            .descendingMap()
            .values()
            .stream()
            .filter(it ->
                        it.resolvedIncrementalMigration != null ||
                        it.resolvedBaselineMigration != null &&
                        it.resolvedBaselineMigration == pendingBaselineMigration)
            .findFirst()
            .map(it -> it.version);
    }

    private List<MigrationInfo> createSortedMigrationInfoList(ResolvedMigration pendingBaselineMigration,
                                                              Map<VersionedMigrationEntry, MigrationState> versionedMigrations,
                                                              Map<RepeatableMigrationEntry, MigrationState> repeatableMigrations) {
        var result = new ArrayList<MigrationInfo>();
        var versionContext = new VersionContext(migrations.baseline);
        versionedMigrations
            .forEach((entry, state) ->
                         result.add(new MigrationInfoImpl(
                             selectResolvedMigration(entry, pendingBaselineMigration),
                             entry.appliedMigration,
                             validationContext,
                             versionContext,
                             state,
                             entry.shouldNotExecute()))
            );
        repeatableMigrations
            .forEach((entry, state) -> {
                         result.add(new MigrationInfoImpl(
                             entry.resolvedMigration,
                             entry.latestAppliedMigration,
                             validationContext,
                             versionContext,
                             state,
                             entry.shouldNotExecute()));
                         entry.supersededRuns.forEach(
                             it -> result.add(new MigrationInfoImpl(
                                                  entry.resolvedMigration,
                                                  it,
                                                  validationContext,
                                                  versionContext,
                                                  MigrationState.SUPERSEDED,
                                                  entry.shouldNotExecute()
                                              )
                             ));
                     }
            );

        result.sort(MigrationInfo.executionOrder());
        return result;
    }

    private ResolvedMigration selectResolvedMigration(VersionedMigrationEntry entry,
                                                      ResolvedMigration pendingBaselineMigration) {
        if (pendingBaselineMigration != null && entry.resolvedBaselineMigration == pendingBaselineMigration) {
            return entry.resolvedBaselineMigration;
        }
        if (entry.resolvedIncrementalMigration != null) {
            return entry.resolvedIncrementalMigration;
        } else {
            return entry.resolvedBaselineMigration;
        }
    }

    private Optional<ResolvedMigration> findPendingBaselineMigration(@Nullable AppliedMigration currentAppliedVersioned,
                                                                     @Nullable Version resolvedTarget) {
        if (currentAppliedVersioned != null) {
            return Optional.empty();
        }
        return migrations.versionedMigrations.descendingMap()
                                             .values()
                                             .stream()
                                             .filter(it -> it.resolvedBaselineMigration != null &&
                                                           (resolvedTarget == null ||
                                                            resolvedTarget.compareTo(it.version) <= 0) &&
                                                           MigrationPattern.anyMatchOrEmpty(it.version, cherryPick))
                                             .findFirst()
                                             .map(it -> it.resolvedBaselineMigration);
    }

    private Optional<ResolvedMigration> resolvedIncrementalAfter(@Nullable AppliedMigration currentApplied) {
        if (currentApplied == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(migrations.versionedMigrations.higherEntry(currentApplied.getVersion()))
                       .map(it -> it.getValue().resolvedIncrementalMigration);
    }

    private Map<VersionedMigrationEntry, MigrationState> computeStateOfVersionedMigrations(
        @Nullable Version resolvedTarget,
        @Nullable ResolvedMigration pendingBaselineMigration,
        @Nullable Version maxResolvedVersion
    ) {
        return migrations.versionedMigrations.values().stream().collect(toMap(
            Function.identity(),
            it -> stateOfVersionedMigration(
                it.version,
                it.deleted,
                it.appliedMigration != null,
                it.appliedMigration != null && it.appliedMigration.isSuccess(),
                it.outOfOrder,
                it.resolvedBaselineMigration != null || it.resolvedIncrementalMigration != null,
                it.appliedMigration != null && it.appliedMigration.getType().equals(MigrationType.BASELINE),
                resolvedTarget != null && it.version.compareTo(resolvedTarget) > 0,
                (migrations.baseline != null &&
                 it.version.compareTo(migrations.baseline) < 0) ||
                (pendingBaselineMigration != null &&
                 it.version.compareTo(pendingBaselineMigration.getVersion()) < 0),
                it.shouldNotExecute(),
                !MigrationPattern.anyMatchOrEmpty(it.version, cherryPick),
                it.resolvedIncrementalMigration == null &&
                it.resolvedBaselineMigration != null &&
                (pendingBaselineMigration == null || !it.version.equals(pendingBaselineMigration.getVersion())),
                migrations.versionedMigrations.tailMap(it.version, false)
                                              .values()
                                              .stream()
                                              .anyMatch(higher -> higher.appliedMigration != null),
                maxResolvedVersion == null || it.version.compareTo(maxResolvedVersion) > 0
            )
        ));
    }

    private MigrationState stateOfVersionedMigration(Version version,
                                                     boolean isDeleted,
                                                     boolean isApplied,
                                                     boolean isSuccess,
                                                     boolean isOutOfOrder,
                                                     boolean isAvailableLocally,
                                                     boolean isBaselineMarker,
                                                     boolean isAboveTarget,
                                                     boolean isBelowBaseline,
                                                     boolean shouldNotExecute,
                                                     boolean isExcludedByCherryPick,
                                                     boolean onlyNonApplicableBaselineAvailable,
                                                     boolean higherVersionHasBeenApplied,
                                                     boolean isFutureVersion) {
        if (shouldNotExecute || isExcludedByCherryPick) {
            return MigrationState.IGNORED;
        }
        if (isDeleted) {
            return MigrationState.DELETED;
        }
        if (isBaselineMarker) {
            return MigrationState.BASELINE;
        }
        if (isApplied && isSuccess) {
            if (isOutOfOrder) {
                return MigrationState.OUT_OF_ORDER;
            }
            if (isAvailableLocally) {
                return MigrationState.SUCCESS;
            } else {
                if (isFutureVersion) {
                    return MigrationState.FUTURE_SUCCESS;
                } else {
                    return MigrationState.MISSING_SUCCESS;
                }
            }
        } else if (isApplied) {
            if (isAvailableLocally) {
                return MigrationState.FAILED;
            } else {
                if (isFutureVersion) {
                    return MigrationState.FUTURE_FAILED;
                } else {
                    return MigrationState.MISSING_FAILED;
                }
            }
        } else if (isBelowBaseline) {
            return MigrationState.BELOW_BASELINE;
        } else if (isAboveTarget) {
            return MigrationState.ABOVE_TARGET;
        } else {
            if (higherVersionHasBeenApplied) {
                return MigrationState.IGNORED;
            }
            if (onlyNonApplicableBaselineAvailable) {
                throw new MigrateDbException(
                    "Pending migration version " + version +
                    " only exists as a baseline migration, but an incremental migration is required.");
            }
            return MigrationState.PENDING;
        }
    }

    private Map<RepeatableMigrationEntry, MigrationState> computeStateOfRepeatableMigrations() {
        return migrations.repeatableMigrations.values().stream().collect(toMap(
            Function.identity(),
            it -> stateOfLatestRepeatableMigrationRun(
                it.deleted,
                it.latestAppliedMigration != null,
                it.latestAppliedMigration != null && it.latestAppliedMigration.isSuccess(),
                it.resolvedMigration != null,
                it.shouldNotExecute(),
                !MigrationPattern.anyMatchOrEmpty(it.description, cherryPick),
                it.resolvedMigration != null && it.latestAppliedMigration != null &&
                !it.resolvedMigration.checksumMatches(it.latestAppliedMigration.getChecksum())
            )
        ));
    }

    private MigrationState stateOfLatestRepeatableMigrationRun(boolean isDeleted,
                                                               boolean isApplied,
                                                               boolean isSuccess,
                                                               boolean isAvailableLocally,
                                                               boolean shouldNotExecute,
                                                               boolean isExcludedByCherryPick,
                                                               boolean isOutdated) {
        if (shouldNotExecute || isExcludedByCherryPick) {
            return MigrationState.IGNORED;
        }
        if (isDeleted) {
            return MigrationState.DELETED;
        }
        if (isSuccess && isOutdated) {
            return MigrationState.OUTDATED;
        }
        if (isApplied && isSuccess) {
            if (isAvailableLocally) {
                return MigrationState.SUCCESS;
            } else {
                return MigrationState.MISSING_SUCCESS;
            }
        } else if (isApplied) {
            if (isAvailableLocally) {
                return MigrationState.FAILED;
            } else {
                return MigrationState.MISSING_FAILED;
            }
        } else {
            return MigrationState.PENDING;
        }
    }

    private Optional<Version> resolveTarget(@Nullable AppliedMigration currentApplied,
                                            @Nullable ResolvedMigration latestResolved,
                                            @Nullable ResolvedMigration nextResolved) {
        if (target == null) {
            return Optional.empty();
        } else {
            return target.mapVersion(version -> version)
                         .orElseGet(Map.of(
                             CURRENT,
                             () -> Optional.ofNullable(currentApplied).map(AppliedMigration::getVersion),
                             LATEST,
                             () -> Optional.ofNullable(latestResolved).map(ResolvedMigration::getVersion),
                             NEXT,
                             () -> Optional.ofNullable(nextResolved).map(ResolvedMigration::getVersion)
                         ));
        }
    }
}
