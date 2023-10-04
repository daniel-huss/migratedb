/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.api.pattern;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.MigrationState;

import java.util.List;
import java.util.Locale;

public final class ValidatePattern {
    private final String migrationType;
    private final String migrationState;
    public static final List<String> validMigrationTypes = List.of("*", "repeatable", "versioned");
    public static final List<String> validMigrationStates = List.of(
        "*",
        MigrationState.MISSING_SUCCESS.getDisplayName().toLowerCase(Locale.ROOT),
        MigrationState.PENDING.getDisplayName().toLowerCase(Locale.ROOT),
        MigrationState.IGNORED.getDisplayName().toLowerCase(Locale.ROOT),
        MigrationState.FUTURE_SUCCESS.getDisplayName().toLowerCase(Locale.ROOT));

    private ValidatePattern(String migrationType, String migrationState) {
        this.migrationType = migrationType;
        this.migrationState = migrationState;
    }

    public String pattern() {
        return migrationType + ":" + migrationState;
    }

    @Override
    public String toString() {
        return pattern();
    }

    public static ValidatePattern fromPattern(String pattern) {
        String[] patternParts = pattern.split(":");

        if (patternParts.length != 2) {
            throw new MigrateDbException(
                "Invalid pattern '" + pattern + "'. Pattern must be of the form <migration_type>:<migration_state>");
        }

        String migrationType = patternParts[0].trim().toLowerCase(Locale.ROOT);
        String migrationState = patternParts[1].trim().toLowerCase(Locale.ROOT);

        if (!validMigrationTypes.contains(migrationType)) {
            throw new MigrateDbException(
                "Invalid migration type '" + patternParts[0] + "'. Valid types are: " + validMigrationTypes);
        }

        if (!validMigrationStates.contains(migrationState)) {
            throw new MigrateDbException(
                "Invalid migration state '" + patternParts[1] + "'. Valid states are: " + validMigrationStates);
        }

        return new ValidatePattern(migrationType, migrationState);
    }

    public boolean matchesMigration(boolean isVersioned, MigrationState state) {
        if (!state.getDisplayName().equalsIgnoreCase(migrationState) && !migrationState.equals("*")) {
            return false;
        }

        if (migrationType.equals("*")) {
            return true;
        }
        if (isVersioned && migrationType.equalsIgnoreCase("versioned")) {
            return true;
        }
        return !isVersioned && migrationType.equalsIgnoreCase("repeatable");
    }
}
