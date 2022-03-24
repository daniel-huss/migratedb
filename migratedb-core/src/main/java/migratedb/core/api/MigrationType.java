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
package migratedb.core.api;

public enum MigrationType {
    /**
     * Schema creation migration.
     */
    SCHEMA(true, false),
    /**
     * Baseline migration.
     */
    BASELINE(true, false),
    /**
     * Deleted migration.
     */
    DELETE(true, false),
    /**
     * SQL migrations.
     */
    SQL(false, false),
    /**
     * SQL baseline migrations.
     */
    SQL_BASELINE(false, true),
    /**
     * JDBC Java-based migrations.
     */
    JDBC(false, false),
    /**
     * JDBC Java-based baseline migrations.
     */
    JDBC_BASELINE(false, true),
    /**
     * Migrations using custom MigrationResolvers.
     */
    CUSTOM(false, false);

    private final boolean synthetic;
    private final boolean baseline;

    MigrationType(boolean synthetic, boolean baseline) {
        this.synthetic = synthetic;
        this.baseline = baseline;
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
        return valueOf(migrationType);
    }

    /**
     * @return Whether this is a synthetic migration type, which is only ever present in the schema history table, but
     * never discovered by migration resolvers.
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    /**
     * @return Whether this is a baseline migration, which represents all migrations with version <= current baseline
     * migration version.
     */
    public boolean isBaselineMigration() {
        return baseline;
    }
}
