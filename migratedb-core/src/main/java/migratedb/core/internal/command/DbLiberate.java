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
package migratedb.core.internal.command;

import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.api.output.LiberateOutput;
import migratedb.core.api.output.LiberateResult;
import migratedb.core.internal.schemahistory.SchemaHistory;

import java.util.Arrays;
import java.util.List;

/**
 * Converts the schema history table into the format used by MigrateDB.
 */
public class DbLiberate {
    private final SchemaHistory schemaHistory;
    private final Configuration configuration;
    private final Database<?> database;
    private final Schema<?, ?>[] schemas;

    public DbLiberate(SchemaHistory schemaHistory,
                      Configuration configuration,
                      Database<?> database,
                      Schema<?, ?>[] schemas) {
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.database = database;
        this.schemas = schemas;
    }

    public LiberateResult liberate() {
        var fromTable = Arrays.stream(schemas)
                .map(it -> it.getTable(configuration.getOldTable()))
                .filter(Table::exists)
                .findFirst().orElse(null);
        var toTable = schemaHistory.getTable();

        var changes = convertToMigrateDb(fromTable, toTable);
        return CommandResultFactory.createLiberateResult(configuration,
                database,
                toTable.getSchema().getName(),
                toTable.getName(),
                changes);
    }

    private List<LiberateOutput> convertToMigrateDb(Table<?, ?> fromTable, Table<?, ?> toTable) {
        // Development.TODO("Implement :o)");
        // convertChecksum()
        // convertDeletionMarkers()
        return List.of();
    }
}
