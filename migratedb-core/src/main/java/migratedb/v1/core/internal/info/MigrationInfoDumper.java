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
package migratedb.v1.core.internal.info;

import migratedb.v1.core.api.MigrationInfo;
import migratedb.v1.core.internal.util.AsciiTable;
import migratedb.v1.core.internal.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dumps migrations in an ascii-art table in the logs and the console.
 */
public class MigrationInfoDumper {
    /**
     * Prevent instantiation.
     */
    private MigrationInfoDumper() {
        // Do nothing
    }

    /**
     * Dumps the info about all migrations into an ascii table.
     *
     * @param migrationInfos The list of migrationInfos to dump.
     * @return The ascii table, as one big multi-line string.
     */
    public static String dumpToAsciiTable(MigrationInfo[] migrationInfos) {
        List<String> columns = Arrays.asList("Category", "Version", "Description", "Type", "Installed On", "State");

        List<List<String>> rows = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfos) {
            List<String> row = Arrays.asList(
                    getCategory(migrationInfo),
                    getVersionStr(migrationInfo),
                    migrationInfo.getDescription(),
                    migrationInfo.getType().name(),
                    DateTimeUtils.formatDateAsIsoishString(migrationInfo.getInstalledOn()),
                    migrationInfo.getState().getDisplayName()
            );
            rows.add(row);
        }

        var output = new StringBuilder();
        new AsciiTable(columns, rows, true, "", "No migrations found").render(output);
        return output.toString();
    }

    static String getCategory(MigrationInfo migrationInfo) {
        if (migrationInfo.getType().isExclusiveToAppliedMigrations()) {
            return "";
        }
        if (migrationInfo.getVersion() == null) {
            return "Repeatable";
        }

        return "Versioned";
    }

    private static String getVersionStr(MigrationInfo migrationInfo) {
        return migrationInfo.getVersion() == null ? "" : migrationInfo.getVersion().toString();
    }

}
