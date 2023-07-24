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
package migratedb.v1.core.api.output;

import java.util.ArrayList;
import java.util.List;

public class RepairResult extends OperationResult {
    public List<String> repairActions;
    public List<RepairOutput> migrationsRemoved;
    public List<RepairOutput> migrationsDeleted;
    public List<RepairOutput> migrationsAligned;

    public RepairResult(String migratedbVersion, String database) {
        this.migratedbVersion = migratedbVersion;
        this.database = database;
        this.repairActions = new ArrayList<>();
        this.migrationsRemoved = new ArrayList<>();
        this.migrationsDeleted = new ArrayList<>();
        this.migrationsAligned = new ArrayList<>();
        this.operation = "repair";
    }

    public void setRepairActions(CompletedRepairActions completedRepairActions) {
        if (completedRepairActions.removedFailedMigrations) {
            repairActions.add(completedRepairActions.removedMessage());
        }
        if (completedRepairActions.deletedMissingMigrations) {
            repairActions.add(completedRepairActions.deletedMessage());
        }
        if (completedRepairActions.alignedAppliedMigrationChecksums) {
            repairActions.add(completedRepairActions.alignedMessage());
        }
    }
}
