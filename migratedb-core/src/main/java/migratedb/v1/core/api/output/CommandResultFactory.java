/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2024 The MigrateDB contributors
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

import migratedb.v1.core.api.ConnectionProvider;
import migratedb.v1.core.api.ErrorDetails;
import migratedb.v1.core.api.MigrationInfo;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.schemahistory.AppliedMigration;
import migratedb.v1.core.internal.info.BuildInfo;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandResultFactory {
    public static LiberateResult createLiberateResult(Configuration configuration,
                                                      Database database,
                                                      String schemaHistorySchema,
                                                      String schemaHistoryTable,
                                                      List<LiberateAction> changes
    ) {
        return new LiberateResult(BuildInfo.VERSION,
                                  getDatabaseName(configuration.getDataSource(), database),
                                  schemaHistorySchema,
                                  configuration.getOldTable(),
                                  schemaHistoryTable,
                                  changes);
    }

    public static InfoResult createInfoResult(Configuration configuration,
                                              Database database,
                                              MigrationInfo[] migrationInfos,
                                              MigrationInfo current,
                                              boolean allSchemasEmpty) {
        String migratedbVersion = BuildInfo.VERSION;
        String databaseName = getDatabaseName(configuration.getDataSource(), database);

        List<InfoOutput> infoOutputs = new ArrayList<>();
        for (MigrationInfo migrationInfo : migrationInfos) {
            infoOutputs.add(createInfoOutput(migrationInfo));
        }

        var currentSchemaVersion = current == null ? null : current.getVersion();
        String schemaVersion = convertToString(currentSchemaVersion == null ? SchemaHistory.EMPTY_SCHEMA_DESCRIPTION
                                                       : currentSchemaVersion);

        return new InfoResult(
                migratedbVersion,
                databaseName,
                schemaVersion,
                String.join(", ", configuration.getSchemas()),
                infoOutputs,
                allSchemasEmpty);
    }

    public static MigrateResult createMigrateResult(String databaseName,
                                                    Configuration configuration) {
        String migratedbVersion = BuildInfo.VERSION;
        return new MigrateResult(migratedbVersion,
                                 databaseName,
                                 String.join(", ", configuration.getSchemas()));
    }

    public static BaselineResult createBaselineResult(String databaseName) {
        String migratedbVersion = BuildInfo.VERSION;
        return new BaselineResult(migratedbVersion, databaseName);
    }

    public static ValidateResult createValidateResult(String databaseName,
                                                      ErrorDetails errorDetails,
                                                      int validationCount,
                                                      List<ValidateOutput> invalidMigrations,
                                                      List<String> warnings) {
        String migratedbVersion = BuildInfo.VERSION;
        boolean validationSuccessful = errorDetails == null;
        return new ValidateResult(migratedbVersion,
                                  databaseName,
                                  errorDetails,
                                  validationSuccessful,
                                  validationCount,
                                  invalidMigrations,
                                  warnings);
    }

    public static RepairResult createRepairResult(Configuration configuration, Database database) {
        String migratedbVersion = BuildInfo.VERSION;
        return new RepairResult(migratedbVersion, getDatabaseName(configuration.getDataSource(), database));
    }

    public static InfoOutput createInfoOutput(MigrationInfo migrationInfo) {
        return new InfoOutput(getCategory(migrationInfo),
                              convertToString(migrationInfo.getVersion()),
                              migrationInfo.getDescription(),
                              convertToString(migrationInfo.getType()),
                              convertToString(migrationInfo.getInstalledOn()),
                              migrationInfo.getState().getDisplayName(),
                              convertToString(migrationInfo.getPhysicalLocation()),
                              convertToString(migrationInfo.getInstalledBy()),
                              migrationInfo.getExecutionTime() != null ? migrationInfo.getExecutionTime() : 0);
    }

    public static MigrateOutput createMigrateOutput(MigrationInfo migrationInfo, int executionTime) {
        return new MigrateOutput(getCategory(migrationInfo),
                                 convertToString(migrationInfo.getVersion()),
                                 migrationInfo.getDescription(),
                                 convertToString(migrationInfo.getType()),
                                 convertToString(migrationInfo.getPhysicalLocation()),
                                 executionTime);
    }

    public static ValidateOutput createValidateOutput(MigrationInfo migrationInfo, ErrorDetails validateError) {
        return new ValidateOutput(
                convertToString(migrationInfo.getVersion()),
                migrationInfo.getDescription(),
                convertToString(migrationInfo.getPhysicalLocation()),
                validateError);
    }

    public static RepairOutput createRepairOutput(MigrationInfo migrationInfo) {
        return new RepairOutput(
                convertToString(migrationInfo.getVersion()),
                migrationInfo.getDescription(),
                migrationInfo.getPhysicalLocation() != null ? migrationInfo.getPhysicalLocation() : "");
    }

    public static RepairOutput createRepairOutput(AppliedMigration am) {
        return new RepairOutput(convertToString(am.getVersion()),
                                am.getDescription(),
                                "");
    }

    private static String getDatabaseName(ConnectionProvider connectionProvider, Database database) {
        try {
            return database.getCatalog();
        } catch (RuntimeException e) {
            try (Connection connection = connectionProvider.getConnection()) {
                String catalog = connection.getCatalog();
                return catalog != null ? catalog : "";
            } catch (RuntimeException | SQLException e1) {
                return "";
            }
        }
    }

    private static String convertToString(@Nullable Object value) {
        return convertToString(value, Object::toString);
    }

    private static <T> String convertToString(@Nullable T value, Function<? super T, String> transform) {
        if (value == null) {
            return "";
        }
        var transformed = transform.apply(value);
        return transformed == null ? "" : transformed;
    }

    private static String getCategory(MigrationInfo migrationInfo) {
        if (migrationInfo.getType().isExclusiveToAppliedMigrations()) {
            return "";
        }
        if (migrationInfo.getVersion() == null) {
            return "Repeatable";
        }

        return "Versioned";
    }
}
