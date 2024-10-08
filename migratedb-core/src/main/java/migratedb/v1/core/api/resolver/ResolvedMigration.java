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
package migratedb.v1.core.api.resolver;

import migratedb.v1.core.api.Checksum;
import migratedb.v1.core.api.MigrationType;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.executor.MigrationExecutor;

/**
 * Migration resolved through a MigrationResolver. Can be applied against a database.
 */
public interface ResolvedMigration {
    /**
     * @return The version of the database after applying this migration, {@code null} for repeatable migrations.
     */
    Version getVersion();

    /**
     * @return Whether this is a repeatable migration.
     */
    default boolean isRepeatable() {
        return getVersion() == null;
    }

    /**
     * @return The description of the migration.
     */
    String getDescription();

    /**
     * @return The name of the script to execute for this migration, relative to its base (classpath/filesystem)
     * location.
     */
    String getScript();

    /**
     * @return The checksum of the migration. Optional. Can be {@code null} if no unique checksum is computable.
     */
    Checksum getChecksum();

    /**
     * @return The type of migration (INIT, SQL, ...)
     */
    MigrationType getType();

    /**
     * @return Description of the location of the migration (on disk, if possible). Used for more precise error
     * reporting in case of conflict.
     */
    String getLocationDescription();

    /**
     * @return The executor to run this migration.
     */
    MigrationExecutor getExecutor();

    boolean checksumMatches(Checksum checksum);
}
