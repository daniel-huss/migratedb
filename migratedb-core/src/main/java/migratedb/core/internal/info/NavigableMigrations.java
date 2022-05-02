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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.function.Predicate;
import migratedb.core.api.Version;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.resolver.ResolvedMigration;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * All migrations except for DELETED markers in a more easily navigable data structure.
 */
final class NavigableMigrations {
    @Nullable final AppliedMigration schemaCreationMarker;
    final @Nullable Version baseline;
    final Map<String, RepeatableMigrationEntry> repeatableMigrations;
    final NavigableMap<Version, VersionedMigrationEntry> versionedMigrations;

    NavigableMigrations(@Nullable AppliedMigration schemaCreationMarker,
                        Map<String, RepeatableMigrationEntry> repeatableMigrations,
                        NavigableMap<Version, VersionedMigrationEntry> versionedMigrations) {
        this.schemaCreationMarker = schemaCreationMarker;
        this.repeatableMigrations = repeatableMigrations;
        this.versionedMigrations = versionedMigrations;
        this.baseline = versionedMigrations.values()
                                           .stream()
                                           .filter(it -> it.appliedMigration != null &&
                                                         it.appliedMigration.isSuccess() &&
                                                         it.appliedMigration.getType().isBaselineMigration())
                                           .max(Comparator.comparing(it -> it.version))
                                           .map(it -> it.version)
                                           .orElse(null);
    }

    Optional<ResolvedMigration> latestResolvedVersioned() {
        return versionedMigrations
            .descendingMap()
            .values()
            .stream()
            .filter(it -> it.resolvedBaselineMigration != null ||
                          it.resolvedIncrementalMigration != null && !it.deleted)
            .findFirst()
            .map(it -> it.resolvedIncrementalMigration == null ? it.resolvedBaselineMigration
                                                               : it.resolvedIncrementalMigration);
    }

    Optional<AppliedMigration> latestAppliedVersioned() {
        return latestAppliedVersioned(it -> true);
    }

    Optional<AppliedMigration> latestAppliedVersioned(Predicate<AppliedMigration> filter) {
        return versionedMigrations
            .descendingMap()
            .values()
            .stream()
            .filter(it -> it.appliedMigration != null && filter.test(it.appliedMigration))
            .findFirst()
            .map(it -> it.appliedMigration);
    }

    /**
     * Correlated info about resolved and applied migrations for a non-repeatable migration with a certain version
     * number.
     */
    static final class VersionedMigrationEntry {
        final Version version;
        @Nullable final ResolvedMigration resolvedIncrementalMigration;
        @Nullable final ResolvedMigration resolvedBaselineMigration;
        @Nullable final AppliedMigration appliedMigration;
        final boolean deleted;
        final boolean outOfOrder;

        VersionedMigrationEntry(Version version,
                                @Nullable ResolvedMigration resolvedIncrementalMigration,
                                @Nullable ResolvedMigration resolvedBaselineMigration,
                                @Nullable AppliedMigration appliedMigration,
                                boolean deleted,
                                boolean outOfOrder) {
            this.version = version;
            this.resolvedIncrementalMigration = resolvedIncrementalMigration;
            this.resolvedBaselineMigration = resolvedBaselineMigration;
            this.appliedMigration = appliedMigration;
            this.deleted = deleted;
            this.outOfOrder = outOfOrder;
        }

        boolean shouldNotExecute() {
            var shouldExecuteBaseline = resolvedBaselineMigration == null ||
                                        resolvedBaselineMigration.getExecutor().shouldExecute();
            var shouldExecuteIncremental = resolvedIncrementalMigration == null ||
                                           resolvedIncrementalMigration.getExecutor().shouldExecute();
            return !shouldExecuteBaseline && !shouldExecuteIncremental;
        }
    }

    /**
     * Correlated info about resolved and applied migrations for a repeatable migration with a certain description.
     */
    static final class RepeatableMigrationEntry {
        final String description;
        @Nullable final ResolvedMigration resolvedMigration;
        final AppliedMigration latestAppliedMigration;
        final List<AppliedMigration> supersededRuns;
        final boolean deleted;

        RepeatableMigrationEntry(String description,
                                 @Nullable ResolvedMigration resolvedMigration,
                                 AppliedMigration latestAppliedMigration,
                                 List<AppliedMigration> supersededRuns,
                                 boolean deleted) {
            this.description = description;
            this.resolvedMigration = resolvedMigration;
            this.latestAppliedMigration = latestAppliedMigration;
            this.supersededRuns = supersededRuns;
            this.deleted = deleted;
        }

        boolean shouldNotExecute() {
            return resolvedMigration != null && !resolvedMigration.getExecutor().shouldExecute();
        }
    }
}
