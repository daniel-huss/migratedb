/*
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

package migratedb.v1.core.internal.info;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.MigrationType;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.internal.schemahistory.AppliedMigration;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.internal.info.NavigableMigrations.RepeatableMigrationEntry;
import migratedb.v1.core.internal.info.NavigableMigrations.VersionedMigrationEntry;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

class NavigableMigrationsBuilder {
    private final List<ResolvedMigration> allResolvedMigrations;
    private final List<AppliedMigration> allAppliedMigrationsInExecutionOrder;
    private final Set<Version> deletedVersions;
    private final Set<String> deletedRepeatableDescriptions;
    private final Set<Version> outOfOrderVersions;

    NavigableMigrationsBuilder(Collection<ResolvedMigration> allResolvedMigrations,
                               Collection<AppliedMigration> allAppliedMigrations) {
        this.allResolvedMigrations = List.copyOf(allResolvedMigrations);
        this.allAppliedMigrationsInExecutionOrder = allAppliedMigrations
            .stream()
            .sorted(comparing(AppliedMigration::getInstalledRank))
            .collect(toList());
        this.deletedVersions = allAppliedMigrationsInExecutionOrder
            .stream()
            .filter(it -> MigrationType.DELETED.equals(it.getType()) && it.getVersion() != null)
            .map(AppliedMigration::getVersion)
            .collect(toSet());
        this.deletedRepeatableDescriptions = allAppliedMigrationsInExecutionOrder
            .stream()
            .filter(it -> MigrationType.DELETED.equals(it.getType()) && it.getVersion() == null)
            .map(AppliedMigration::getDescription)
            .collect(toSet());
        outOfOrderVersions = new HashSet<>();
        Version prev = null;
        for (var applied : allAppliedMigrationsInExecutionOrder) {
            var next = applied.getVersion();
            if (next != null) {
                if (prev != null && prev.compareTo(next) >= 0) {
                    outOfOrderVersions.add(next);
                }
                prev = next;
            }
        }
    }

    NavigableMigrations build() {
        var repeatableInfo = infoAboutRepeatableMigrations();
        var versionedInfo = infoAboutVersionedMigrations();
        var schemaCreationMarker = allAppliedMigrationsInExecutionOrder
            .stream()
            .filter(it -> MigrationType.SCHEMA.equals(it.getType()))
            .collect(toList());
        if (schemaCreationMarker.size() > 1) {
            throw new MigrateDbException("Schema history corrupted: More than one schema creation marker found");
        }
        return new NavigableMigrations(
                repeatableInfo,
                                       new TreeMap<>(versionedInfo));
    }

    private Map<Version, VersionedMigrationEntry> infoAboutVersionedMigrations() {
        var appliedVersions = appliedVersionedMigrations()
            .stream()
            .map(AppliedMigration::getVersion);
        var resolvedIncrementalVersions = resolvedIncrementalMigrations()
            .values()
            .stream()
            .map(ResolvedMigration::getVersion);
        var resolvedBaselineVersions = resolvedBaselineMigrations()
            .values()
            .stream()
            .map(ResolvedMigration::getVersion);
        return Stream.of(appliedVersions, resolvedIncrementalVersions, resolvedBaselineVersions)
                     .flatMap(identity())
                     .distinct()
                     .map(this::gatherVersionedMigrationInfo)
                     .collect(toMap(it -> it.version, identity()));
    }

    private Map<String, RepeatableMigrationEntry> infoAboutRepeatableMigrations() {
        var appliedDescriptions = appliedRepeatableMigrations()
            .stream()
            .map(AppliedMigration::getDescription);
        var resolvedDescriptions = resolvedRepeatableMigrations()
            .values()
            .stream()
            .map(ResolvedMigration::getDescription);
        return Stream.of(appliedDescriptions, resolvedDescriptions)
                     .flatMap(identity())
                     .distinct()
                     .map(this::gatherRepeatableMigrationInfo)
                     .collect(toMap(it -> it.description, identity()));
    }

    private VersionedMigrationEntry gatherVersionedMigrationInfo(Version version) {
        var resolvedBaseline = resolvedBaselineMigrations().get(version);
        var resolvedIncremental = resolvedIncrementalMigrations().get(version);
        var applied = appliedVersionedMigrations().stream()
                                                  .filter(it -> version.equals(it.getVersion()))
                                                  .collect(toList());
        if (applied.size() > 1) {
            throw new MigrateDbException(
                "Schema history corrupted: More than one applied migration exists for version " + version);
        }
        var deleted = deletedVersions.contains(version);
        var outOfOrder = outOfOrderVersions.contains(version);
        return new VersionedMigrationEntry(version,
                                           resolvedIncremental,
                                           resolvedBaseline,
                                           applied.isEmpty() ? null : applied.get(0),
                                           deleted,
                                           outOfOrder);
    }

    private RepeatableMigrationEntry gatherRepeatableMigrationInfo(String description) {
        var resolved = resolvedRepeatableMigrations().get(description);
        var allApplied = appliedRepeatableMigrations()
            .stream()
            .filter(it -> description.equals(it.getDescription()))
            .sorted(Comparator.comparing(AppliedMigration::getInstalledRank))
            .collect(toList());
        var deleted = deletedRepeatableDescriptions.contains(description);
        var supersededRuns = allApplied.isEmpty() ? List.<AppliedMigration>of()
                                                  : allApplied.subList(0, allApplied.size() - 1);
        var latestApplied = allApplied.isEmpty() ? null : allApplied.get(allApplied.size() - 1);
        return new RepeatableMigrationEntry(description,
                                            resolved,
                                            latestApplied,
                                            supersededRuns,
                                            deleted);
    }

    private List<AppliedMigration> appliedRepeatableMigrations() {
        return computeOnce(appliedRepeatableMigrations,
                           () -> allAppliedMigrationsInExecutionOrder
                               .stream()
                               .filter(AppliedMigration::isExecutionOfRepeatableMigration)
                               .collect(toList()));
    }

    private List<AppliedMigration> appliedVersionedMigrations() {
        return computeOnce(appliedIncrementalOrBaselineMigrations,
                           () -> allAppliedMigrationsInExecutionOrder
                               .stream()
                               .filter(it -> !MigrationType.SCHEMA.equals(it.getType()) &&
                                             !it.isExecutionOfRepeatableMigration())
                               .collect(toList()));
    }

    private Map<String, ResolvedMigration> resolvedRepeatableMigrations() {
        return computeOnce(resolvedRepeatableMigrations,
                           () -> allResolvedMigrations
                               .stream()
                               .filter(ResolvedMigration::isRepeatable)
                               .collect(toMap(ResolvedMigration::getDescription, identity())));
    }

    private Map<Version, ResolvedMigration> resolvedIncrementalMigrations() {
        return computeOnce(resolvedIncrementalMigrations,
                           () -> allResolvedMigrations
                               .stream()
                               .filter(it -> !it.isRepeatable() &&
                                             !it.getType().isBaselineMigration())
                               .collect(toMap(ResolvedMigration::getVersion, identity())));
    }

    private Map<Version, ResolvedMigration> resolvedBaselineMigrations() {
        return computeOnce(resolvedBaselineMigrations,
                           () -> allResolvedMigrations
                               .stream()
                               .filter(it -> it.getType().isBaselineMigration())
                               .collect(toMap(ResolvedMigration::getVersion, identity())));
    }

    private static <T> T computeOnce(Holder<T> holder, Supplier<T> computation) {
        var result = holder.value;
        if (result == null) {
            result = computation.get();
            holder.value = result;
        }
        return result;
    }

    private static final class Holder<T> {
        T value;
    }

    // Containers for lazily computed values, do not access them directly!

    private final Holder<List<AppliedMigration>> appliedRepeatableMigrations = new Holder<>();
    private final Holder<List<AppliedMigration>> appliedIncrementalOrBaselineMigrations = new Holder<>();
    private final Holder<Map<String, ResolvedMigration>> resolvedRepeatableMigrations = new Holder<>();
    private final Holder<Map<Version, ResolvedMigration>> resolvedIncrementalMigrations = new Holder<>();
    private final Holder<Map<Version, ResolvedMigration>> resolvedBaselineMigrations = new Holder<>();
}
