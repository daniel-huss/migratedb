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

import migratedb.core.api.MigrationInfo;
import migratedb.core.api.MigrationInfoService;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationState.Category;

import java.util.Comparator;

/**
 * The ordering used by {@link MigrationInfoService}. It places
 * <ul>
 *     <li>applied migrations before resolved pending migrations</li>
 *     <li>lower versions before higher versions for pending migrations</li>
 *     <li>versioned migrations before repeatable migrations</li>
 * </ul>
 */
public final class MigrationExecutionOrdering implements Comparator<MigrationInfo> {
    @Override
    public int compare(MigrationInfo o1, MigrationInfo o2) {
        if ((o1.getInstalledRank() != null) && (o2.getInstalledRank() != null)) {
            return o1.getInstalledRank().compareTo(o2.getInstalledRank());
        }

        MigrationState state = o1.getState();
        MigrationState oState = o2.getState();

        // Below baseline migrations come before applied ones
        if (state == MigrationState.BELOW_BASELINE && oState.is(Category.APPLIED)) {
            return -1;
        }
        if (state.is(Category.APPLIED) && oState == MigrationState.BELOW_BASELINE) {
            return 1;
        }

        // Sort installed before pending
        if (o1.getInstalledRank() != null) {
            return -1;
        }
        if (o2.getInstalledRank() != null) {
            return 1;
        }

        return compareVersion(o1, o2);
    }

    private int compareVersion(MigrationInfo o1, MigrationInfo o2) {
        if (o1.getVersion() != null && o2.getVersion() != null) {
            return o1.getVersion().compareTo(o2.getVersion());
        }

        // One versioned and one repeatable migration: versioned migration goes before repeatable
        if (o1.getVersion() != null) {
            return -1;
        }
        if (o2.getVersion() != null) {
            return 1;
        }

        // Two repeatable migrations: sort by description
        return o1.getDescription().compareTo(o2.getDescription());
    }
}
