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
package migratedb.v1.core.api;

public enum MigrationType {
    /**
     * Schema creation marker.
     */
    SCHEMA(true, false),
    /**
     * Baseline marker inserted by the baseline command. Its presence means that the current database state was accepted
     * as the corresponding version without actually making any changes.
     */
    BASELINE(true, false),
    /**
     * A migration that has been deleted by the repair command.
     */
    DELETED(true, false),
    /**
     * SQL incremental migrations.
     */
    SQL(false, false),
    /**
     * SQL baseline migrations.
     */
    SQL_BASELINE(false, true),
    /**
     * JDBC Java-based incremental migrations.
     */
    JDBC(false, false),
    /**
     * JDBC Java-based baseline migrations.
     */
    JDBC_BASELINE(false, true);

    private final boolean isExclusiveToAppliedMigrations;
    private final boolean baselineMigration;

    MigrationType(boolean isExclusiveToAppliedMigrations, boolean baselineMigration) {
        this.isExclusiveToAppliedMigrations = isExclusiveToAppliedMigrations;
        this.baselineMigration = baselineMigration;
    }

    public static MigrationType fromString(String migrationType) {
        // Convert legacy types to maintain compatibility
        if ("SPRING_JDBC".equals(migrationType)) {
            return JDBC;
        }
        if ("SQL_STATE_SCRIPT".equals(migrationType)) {
            return SQL_BASELINE;
        }
        if ("JDBC_STATE_SCRIPT".equals(migrationType)) {
            return JDBC_BASELINE;
        }
        if ("DELETE".equals(migrationType)) {
            return DELETED;
        }
        return valueOf(migrationType);
    }

    /**
     * @return Whether this migration type is only ever present in the schema history table, but never discovered by
     * migration resolvers.
     */
    public boolean isExclusiveToAppliedMigrations() {
        return isExclusiveToAppliedMigrations;
    }

    /**
     * @return Whether this is a baseline migration, which represents all migrations with version {@code ≤} current baseline
     * migration version. Note that the special baseline marker {@link #BASELINE} is not a real migration, and therefore
     * not a baseline migration.
     */
    public boolean isBaselineMigration() {
        return baselineMigration;
    }
}
