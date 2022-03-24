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

package migratedb.core.api;

import java.time.Instant;
import java.util.Comparator;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.info.MigrationExecutionOrdering;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Info about a migration.
 */
public interface MigrationInfo {
    static Comparator<MigrationInfo> executionOrder() {
        return new MigrationExecutionOrdering();
    }

    /**
     * @return Whether this is a repeatable migration.
     */
    default boolean isRepeatable() {
        return getVersion() == null;
    }

    /**
     * @return The resolved migration to aggregate the info from.
     */
    @Nullable ResolvedMigration getResolvedMigration();

    /**
     * @return The applied migration to aggregate the info from.
     */
    @Nullable AppliedMigration getAppliedMigration();

    /**
     * @return The type of migration (BASELINE, SQL, JDBC, ...)
     */
    MigrationType getType();

    /**
     * @return The target version of this migration.
     */
    @Nullable Integer getChecksum();

    /**
     * @return The schema version after the migration is complete.
     */
    @Nullable Version getVersion();

    /**
     * @return The description of the migration.
     */
    String getDescription();

    /**
     * @return The name of the script to execute for this migration, relative to its classpath or filesystem location.
     */
    String getScript();

    /**
     * @return The state of the migration (PENDING, SUCCESS, ...)
     */
    MigrationState getState();

    /**
     * @return The timestamp when this migration was installed. (Only for applied migrations)
     */
    @Nullable Instant getInstalledOn();

    /**
     * @return The user that installed this migration. (Only for applied migrations)
     */
    @Nullable String getInstalledBy();

    /**
     * @return The rank of this installed migration. This is the most precise way to sort applied migrations by
     * installation order. Migrations that were applied later have a higher rank. (Only for applied migrations)
     */
    @Nullable Integer getInstalledRank();

    /**
     * @return The execution time (in millis) of this migration. (Only for applied migrations)
     */
    @Nullable Integer getExecutionTime();

    /**
     * @return The physical location of the migration on disk.
     */
    String getPhysicalLocation();

    /**
     * @return The error code with the relevant validation message, or {@code null} if everything is fine.
     */
    @Nullable ErrorDetails validate();
}
