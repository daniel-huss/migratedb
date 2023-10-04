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
package migratedb.v1.core.api;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * The state of a migration.
 */
public enum MigrationState {
    /**
     * This migration has not been applied, yet.
     */
    PENDING("Pending", Category.RESOLVED),
    /**
     * This migration has not been applied yet, and won't be applied because target is set to a lower version.
     */
    ABOVE_TARGET("Above Target", Category.RESOLVED),
    /**
     * This migration was not applied against this DB, because the schema history table was baselined with a higher
     * version.
     */
    BELOW_BASELINE("Below Baseline", Category.RESOLVED),
    /**
     * The current DB state was accepted as the corresponding version without making any changes.
     */
    BASELINE("Baseline", Category.RESOLVED, Category.APPLIED),
    /**
     * When using cherryPick, this indicates a migration that was not in the cherry picked list. When not using
     * cherryPick, this usually indicates a problem: The migration was ignored because a higher version has already been
     * applied.
     */
    IGNORED("Ignored", Category.RESOLVED),
    /**
     * This migration succeeded.
     * <p>
     * This migration was applied against this DB, but it is not available locally. This usually results from multiple
     * older migration files being consolidated into a single one.
     */
    MISSING_SUCCESS("Missing", Category.APPLIED, Category.MISSING),
    /**
     * This migration failed.
     * <p>
     * This migration was applied against this DB, but it is not available locally. This usually results from multiple
     * older migration files being consolidated into a single one.
     */
    MISSING_FAILED("Failed (Missing)", Category.APPLIED, Category.FAILED, Category.MISSING),
    /**
     * This migration succeeded.
     */
    SUCCESS("Success", Category.RESOLVED, Category.APPLIED),
    /**
     * This migration failed.
     */
    FAILED("Failed", Category.RESOLVED, Category.APPLIED, Category.FAILED),
    /**
     * This migration succeeded.
     * <p>
     * This migration succeeded, but it was applied out of order. Rerunning the entire migration history might produce
     * different results!
     */
    OUT_OF_ORDER("Out of Order", Category.RESOLVED, Category.APPLIED),
    /**
     * This migration succeeded.
     * <p>
     * This migration has been applied against the DB, but it is not available locally. Its version is higher than the
     * highest version available locally. It was most likely successfully installed by a future version of this
     * deployable.
     */
    FUTURE_SUCCESS("Future", Category.APPLIED, Category.FUTURE),
    /**
     * This migration failed.
     * <p>
     * This migration has been applied against the DB, but it is not available locally. Its version is higher than the
     * highest version available locally. It most likely failed during the installation of a future version of this
     * deployable.
     */
    FUTURE_FAILED("Failed (Future)", Category.APPLIED, Category.FUTURE, Category.FAILED),
    /**
     * This is a repeatable migration that has been applied, but is outdated and should be re-applied.
     */
    OUTDATED("Outdated", Category.RESOLVED, Category.APPLIED),
    /**
     * This is a repeatable migration that is outdated and has already been superseded by a newer run.
     */
    SUPERSEDED("Superseded", Category.RESOLVED, Category.APPLIED),
    /**
     * This is a migration that has been marked as deleted.
     */
    DELETED("Deleted", Category.APPLIED),
    ;

    public enum Category {
        RESOLVED, APPLIED, FAILED, FUTURE, MISSING
    }

    private final String displayName;
    private final EnumSet<Category> categories;

    MigrationState(String displayName, Category... categories) {
        this.displayName = displayName;
        this.categories = EnumSet.noneOf(Category.class);
        this.categories.addAll(Arrays.asList(categories));
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean is(Category category) {
        return categories.contains(category);
    }
}
