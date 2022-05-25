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
package migratedb.core.api.internal.schemahistory;

import migratedb.core.api.Checksum;
import migratedb.core.api.MigrationType;
import migratedb.core.api.Version;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

/**
 * A migration applied to the database (maps to a row in the schema history table).
 * <p>
 * Its natural ordering is inconsistent with equals().
 */
public final class AppliedMigration {
    /**
     * The order in which this migration was applied amongst all others. (For out of order detection)
     */
    private final int installedRank;

    /**
     * The target version of this migration. {@code null} if it is a repeatable migration.
     */
    private final @Nullable Version version;

    /**
     * The description of the migration.
     */
    private final String description;

    /**
     * The type of migration (BASELINE, SQL, ...)
     */
    private final MigrationType type;

    /**
     * The name of the script to execute for this migration, relative to its classpath location.
     */
    private final String script;

    /**
     * The checksum of the migration. (Optional)
     */
    private final @Nullable Checksum checksum;

    /**
     * The timestamp when this migration was installed.
     */
    private final Instant installedOn;

    /**
     * The user that installed this migration.
     */
    private final String installedBy;

    /**
     * The execution time (in millis) of this migration.
     */
    private final int executionTime;

    /**
     * Flag indicating whether the migration was successful or not.
     */
    private final boolean success;

    /**
     * Creates a new applied migration. Only called from the RowMapper.
     *
     * @param installedRank The order in which this migration was applied amongst all others. (For out of order
     *                      detection)
     * @param version       The target version of this migration.
     * @param description   The description of the migration.
     * @param type          The type of migration (INIT, SQL, ...)
     * @param script        The name of the script to execute for this migration, relative to its classpath location.
     * @param checksum      The checksum of the migration. (Optional)
     * @param installedOn   The timestamp when this migration was installed.
     * @param installedBy   The user that installed this migration.
     * @param executionTime The execution time (in millis) of this migration.
     * @param success       Flag indicating whether the migration was successful or not.
     */
    public AppliedMigration(int installedRank,
                            @Nullable Version version,
                            String description,
                            MigrationType type,
                            String script,
                            @Nullable Checksum checksum,
                            Timestamp installedOn,
                            String installedBy,
                            int executionTime,
                            boolean success) {
        this.installedRank = installedRank;
        this.version = version;
        this.description = description;
        this.type = type;
        this.script = script;
        this.checksum = checksum;
        this.installedOn = installedOn == null ? null : installedOn.toInstant();
        this.installedBy = installedBy;
        this.executionTime = executionTime;
        this.success = success;
    }

    /**
     * @return The order in which this migration was applied amongst all others. (For out of order detection)
     */
    public int getInstalledRank() {
        return installedRank;
    }

    /**
     * @return The target version of this migration.
     */
    public @Nullable Version getVersion() {
        return version;
    }

    /**
     * @return The description of the migration.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The type of migration (BASELINE, SQL, ...)
     */
    public MigrationType getType() {
        return type;
    }

    /**
     * @return The name of the script to execute for this migration, relative to its classpath location.
     */
    public String getScript() {
        return script;
    }

    /**
     * @return The checksum of the migration. (Optional)
     */
    public @Nullable Checksum getChecksum() {
        return checksum;
    }

    /**
     * @return The timestamp when this migration was installed.
     */
    public Instant getInstalledOn() {
        return installedOn;
    }

    /**
     * @return The user that installed this migration.
     */
    public String getInstalledBy() {
        return installedBy;
    }

    /**
     * @return The execution time (in millis) of this migration.
     */
    public int getExecutionTime() {
        return executionTime;
    }

    /**
     * @return Flag indicating whether the migration was successful or not.
     */
    public boolean isSuccess() {
        return success;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppliedMigration)) {
            return false;
        }
        AppliedMigration other = (AppliedMigration) o;
        return (executionTime == other.executionTime) &&
                (installedRank == other.installedRank) &&
                (success == other.success) &&
                Objects.equals(checksum, other.checksum) &&
                Objects.equals(description, other.description) &&
                Objects.equals(installedBy, other.installedBy) &&
                Objects.equals(installedOn, other.installedOn) &&
                Objects.equals(script, other.script) &&
                Objects.equals(type, other.type) &&
                Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installedRank,
                version,
                description,
                type,
                script,
                checksum,
                installedOn,
                installedBy,
                executionTime,
                success);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "installedRank=" + installedRank +
                ", version=" + version +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", checksum=" + checksum +
                ", installedOn=" + installedOn +
                ", installedBy='" + installedBy + '\'' +
                ", success=" + success +
                '}';
    }

    public boolean isExecutionOfRepeatableMigration() {
        return getVersion() == null &&
                !getType().equals(MigrationType.BASELINE) &&
                !getType().equals(MigrationType.SCHEMA);
    }
}
