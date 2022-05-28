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
package migratedb.core.api.output;

import java.util.List;

public class LiberateResult extends OperationResult {
    public String schemaHistorySchema;
    public String oldSchemaHistoryTable;
    public String schemaHistoryTable;
    public List<LiberateOutput> changes;

    public LiberateResult(String migratedbVersion,
                          String database,
                          String schemaHistorySchema,
                          String oldSchemaHistoryTable,
                          String schemaHistoryTable,
                          List<LiberateOutput> changes) {
        this.schemaHistorySchema = schemaHistorySchema;
        this.oldSchemaHistoryTable = oldSchemaHistoryTable;
        this.schemaHistoryTable = schemaHistoryTable;
        this.changes = changes;
        this.migratedbVersion = migratedbVersion;
        this.database = database;
        this.operation = "liberate";
    }
}
