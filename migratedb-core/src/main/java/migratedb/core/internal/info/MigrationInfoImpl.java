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

import java.time.Instant;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.ErrorDetails;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationState.Category;
import migratedb.core.api.MigrationType;
import migratedb.core.api.Version;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.AbbreviationUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

final class MigrationInfoImpl implements MigrationInfo {
    private final @Nullable ResolvedMigration resolvedMigration;
    private final @Nullable AppliedMigration appliedMigration;
    private final ValidationContext validationContext;
    private final VersionContext versionContext;
    private final MigrationState state;
    private final boolean shouldNotExecuteMigration;

    MigrationInfoImpl(@Nullable ResolvedMigration resolvedMigration,
                      @Nullable AppliedMigration appliedMigration,
                      ValidationContext validationContext,
                      VersionContext versionContext,
                      MigrationState state,
                      boolean shouldNotExecuteMigration) {
        if (resolvedMigration == null && appliedMigration == null) {
            throw new IllegalArgumentException("One of (resolved migration, applied migration) must not be null");
        }
        this.resolvedMigration = resolvedMigration;
        this.appliedMigration = appliedMigration;
        this.validationContext = validationContext;
        this.versionContext = versionContext;
        this.state = state;
        this.shouldNotExecuteMigration = shouldNotExecuteMigration;
    }

    @Override
    public ResolvedMigration getResolvedMigration() {
        return resolvedMigration;
    }

    @Override
    public AppliedMigration getAppliedMigration() {
        return appliedMigration;
    }

    @Override
    public MigrationType getType() {
        if (appliedMigration != null) {
            return appliedMigration.getType();
        }
        return resolvedMigration.getType();
    }

    @Override
    public Integer getChecksum() {
        if (appliedMigration != null) {
            return appliedMigration.getChecksum();
        }
        return resolvedMigration.getChecksum();
    }

    @Override
    public Version getVersion() {
        if (appliedMigration != null) {
            return appliedMigration.getVersion();
        }
        return resolvedMigration.getVersion();
    }

    @Override
    public String getDescription() {
        if (appliedMigration != null) {
            return appliedMigration.getDescription();
        }
        return resolvedMigration.getDescription();
    }

    @Override
    public String getScript() {
        if (appliedMigration != null) {
            return appliedMigration.getScript();
        }
        return resolvedMigration.getScript();
    }

    @Override
    public MigrationState getState() {
        return state;
    }

    @Override
    public Instant getInstalledOn() {
        if (appliedMigration != null) {
            return appliedMigration.getInstalledOn();
        }
        return null;
    }

    @Override
    public String getInstalledBy() {
        if (appliedMigration != null) {
            return appliedMigration.getInstalledBy();
        }
        return null;
    }

    @Override
    public Integer getInstalledRank() {
        if (appliedMigration != null) {
            return appliedMigration.getInstalledRank();
        }
        return null;
    }

    @Override
    public Integer getExecutionTime() {
        if (appliedMigration != null) {
            return appliedMigration.getExecutionTime();
        }
        return null;
    }

    @Override
    public String getPhysicalLocation() {
        if (resolvedMigration != null) {
            return resolvedMigration.getLocationDescription();
        }
        return "";
    }

    @Override
    public @Nullable ErrorDetails validate() {
        MigrationState state = getState();

        if (MigrationState.ABOVE_TARGET.equals(state)) {
            return null;
        }

        if (MigrationState.DELETED.equals(state)) {
            return null;
        }

        if (state.is(Category.FAILED) &&
            (!validationContext.allows(ValidationMatch.FUTURE) || MigrationState.FUTURE_FAILED != state)) {
            if (getVersion() == null) {
                String errorMessage = "Detected failed repeatable migration: " + getDescription() +
                                      ". Please remove any half-completed changes then run repair to fix the schema " +
                                      "history.";
                return new ErrorDetails(ErrorCode.FAILED_REPEATABLE_MIGRATION, errorMessage);
            }
            String errorMessage =
                "Detected failed migration to version " + getVersion() + " (" + getDescription() + ")" +
                ". Please remove any half-completed changes then run repair to fix the schema history.";
            return new ErrorDetails(ErrorCode.FAILED_VERSIONED_MIGRATION, errorMessage);
        }

        if ((resolvedMigration == null)
            && !appliedMigration.getType().isSynthetic()

            && (MigrationState.SUPERSEDED != state)
            && (!validationContext.allows(ValidationMatch.MISSING) ||
                (MigrationState.MISSING_SUCCESS != state && MigrationState.MISSING_FAILED != state))
            && (!validationContext.allows(ValidationMatch.FUTURE) ||
                (MigrationState.FUTURE_SUCCESS != state && MigrationState.FUTURE_FAILED != state))) {
            if (appliedMigration.getVersion() != null) {
                String errorMessage = "Detected applied migration not resolved locally: " + getVersion() +
                                      ". If you removed this migration intentionally, run repair to mark the " +
                                      "migration as deleted.";
                return new ErrorDetails(ErrorCode.APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED, errorMessage);
            } else {
                String errorMessage = "Detected applied migration not resolved locally: " + getDescription() +
                                      ". If you removed this migration intentionally, run repair to mark the " +
                                      "migration as deleted.";
                return new ErrorDetails(ErrorCode.APPLIED_REPEATABLE_MIGRATION_NOT_RESOLVED, errorMessage);
            }
        }

        if (!validationContext.allows(ValidationMatch.IGNORED) && MigrationState.IGNORED == state) {
            if (shouldNotExecuteMigration) {
                return null;
            }
            if (getVersion() != null) {
                String errorMessage = "Detected resolved migration not applied to database: " + getVersion() +
                                      ". To ignore this migration, set -ignoreIgnoredMigrations=true. To allow " +
                                      "executing this migration, set -outOfOrder=true.";
                return new ErrorDetails(ErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED, errorMessage);
            }
            String errorMessage =
                "Detected resolved repeatable migration not applied to database: " + getDescription() +
                ". To ignore this migration, set -ignoreIgnoredMigrations=true.";
            return new ErrorDetails(ErrorCode.RESOLVED_REPEATABLE_MIGRATION_NOT_APPLIED, errorMessage);
        }

        if (!validationContext.allows(ValidationMatch.PENDING) && MigrationState.PENDING == state) {
            if (getVersion() != null) {
                String errorMessage = "Detected resolved migration not applied to database: " + getVersion() +
                                      ". To fix this error, either run migrate, or set -ignorePendingMigrations=true.";
                return new ErrorDetails(ErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED, errorMessage);
            }
            String errorMessage =
                "Detected resolved repeatable migration not applied to database: " + getDescription() +
                ". To fix this error, either run migrate, or set -ignorePendingMigrations=true.";
            return new ErrorDetails(ErrorCode.RESOLVED_REPEATABLE_MIGRATION_NOT_APPLIED, errorMessage);
        }

        if (!validationContext.allows(ValidationMatch.PENDING) && MigrationState.OUTDATED == state) {
            String errorMessage =
                "Detected outdated resolved repeatable migration that should be re-applied to database: " +
                getDescription() + ". Run migrate to execute this migration.";
            return new ErrorDetails(ErrorCode.OUTDATED_REPEATABLE_MIGRATION, errorMessage);
        }

        if (resolvedMigration != null && appliedMigration != null
            && getType() != MigrationType.DELETED

        ) {
            String migrationIdentifier = appliedMigration.getVersion() == null ?
                                         // Repeatable migrations
                                         appliedMigration.getScript() :
                                         // Versioned migrations
                                         "version " + appliedMigration.getVersion();
            if (getVersion() == null || getVersion().compareTo(versionContext.baselineVersion) > 0) {
                if (resolvedMigration.getType() != appliedMigration.getType()) {
                    String mismatchMessage = createMismatchMessage("type",
                                                                   migrationIdentifier,
                                                                   appliedMigration.getType(),
                                                                   resolvedMigration.getType());
                    return new ErrorDetails(ErrorCode.TYPE_MISMATCH, mismatchMessage);
                }
                if (resolvedMigration.getVersion() != null
                    || (validationContext.allows(ValidationMatch.PENDING) && MigrationState.OUTDATED != state &&
                        MigrationState.SUPERSEDED != state)) {
                    if (!resolvedMigration.checksumMatches(appliedMigration.getChecksum())) {
                        String mismatchMessage = createMismatchMessage("checksum",
                                                                       migrationIdentifier,
                                                                       appliedMigration.getChecksum(),
                                                                       resolvedMigration.getChecksum());
                        return new ErrorDetails(ErrorCode.CHECKSUM_MISMATCH, mismatchMessage);
                    }
                }
                if (descriptionMismatch(resolvedMigration, appliedMigration)) {
                    String mismatchMessage = createMismatchMessage("description",
                                                                   migrationIdentifier,
                                                                   appliedMigration.getDescription(),
                                                                   resolvedMigration.getDescription());
                    return new ErrorDetails(ErrorCode.DESCRIPTION_MISMATCH, mismatchMessage);
                }
            }
        }

        return null;
    }

    private boolean descriptionMismatch(ResolvedMigration resolvedMigration, AppliedMigration appliedMigration) {
        // For some databases, we can't put an empty description into the history table
        if (SchemaHistory.NO_DESCRIPTION_MARKER.equals(appliedMigration.getDescription())) {
            return !"".equals(resolvedMigration.getDescription()) &&
                   !SchemaHistory.NO_DESCRIPTION_MARKER.equals(resolvedMigration.getDescription());
        }
        return (!AbbreviationUtils.abbreviateDescription(resolvedMigration.getDescription())
                                  .equals(appliedMigration.getDescription()));
    }

    private String createMismatchMessage(String mismatch, String migrationIdentifier, Object applied, Object resolved) {
        return String.format("Migration " + mismatch + " mismatch for migration %s\n" +
                             "-> Applied to database : %s\n" +
                             "-> Resolved locally    : %s" +
                             ". Either revert the changes to the migration, or run repair to update the schema " +
                             "history.",
                             migrationIdentifier, applied, resolved);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "resolved.version=" + (resolvedMigration == null ? null : resolvedMigration.getVersion()) +
               ", applied.version=" + (appliedMigration == null ? null : appliedMigration.getVersion()) +
               '}';
    }
}
