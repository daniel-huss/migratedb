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
package migratedb.core.internal.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.MigrationVersion;
import migratedb.core.api.pattern.ValidatePattern;

final class MigrationInfoContext {
    public boolean outOfOrder;
    public boolean pending;
    public boolean missing;
    public boolean ignored;
    public boolean future;
    public ValidatePattern[] ignorePatterns = new ValidatePattern[0];
    public MigrationVersion target;
    public MigrationPattern[] cherryPick;
    public MigrationVersion schema;
    public MigrationVersion baseline;
    public MigrationVersion lastResolved = MigrationVersion.EMPTY;
    public MigrationVersion lastApplied = MigrationVersion.EMPTY;
    public MigrationVersion latestBaselineMigration = MigrationVersion.EMPTY;
    public Map<String, Integer> latestRepeatableRuns = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MigrationInfoContext)) {
            return false;
        }
        MigrationInfoContext other = (MigrationInfoContext) o;
        return (outOfOrder == other.outOfOrder) &&
               (pending == other.pending) &&
               (missing == other.missing) &&
               (ignored == other.ignored) &&
               (future == other.future) && (Objects.equals(target, other.target)) &&
               (Objects.equals(schema, other.schema)) &&
               (Objects.equals(baseline, other.baseline)) &&
               (Objects.equals(lastResolved, other.lastResolved)) &&
               (Objects.equals(lastApplied, other.lastApplied)) &&
               (Arrays.equals(cherryPick, other.cherryPick)) &&
               Objects.equals(latestRepeatableRuns, other.latestRepeatableRuns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            outOfOrder,
            pending,
            missing,
            ignored,
            future,
            target,
            schema,
            baseline,
            lastResolved,
            lastApplied,
            Arrays.hashCode(cherryPick),
            latestRepeatableRuns);
    }
}
