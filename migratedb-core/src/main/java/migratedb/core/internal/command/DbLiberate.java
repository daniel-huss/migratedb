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

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationType;
import migratedb.core.api.Version;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.api.output.LiberateAction;
import migratedb.core.api.output.LiberateResult;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.jdbc.JdbcNullTypes;
import migratedb.core.internal.schemahistory.SchemaHistory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static migratedb.core.internal.schemahistory.SchemaHistory.NO_DESCRIPTION_MARKER;

/**
 * Converts the schema history table into the format used by MigrateDB.
 */
public class DbLiberate {
    private static final Log LOG = Log.getLog(DbLiberate.class);

    private final SchemaHistory schemaHistory;
    private final Configuration configuration;
    private final Database<?> database;
    private final Schema<?, ?> defaultSchema;
    private final Schema<?, ?>[] schemas;
    private final CallbackExecutor callbackExecutor;
    private final JdbcTemplate jdbcTemplate;

    public DbLiberate(SchemaHistory schemaHistory,
                      Configuration configuration,
                      Database<?> database,
                      Schema<?, ?> defaultSchema,
                      Schema<?, ?>[] schemas,
                      CallbackExecutor callbackExecutor) {
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.database = database;
        this.defaultSchema = defaultSchema;
        this.schemas = schemas;
        this.callbackExecutor = callbackExecutor;
        this.jdbcTemplate = database.getMainConnection().getJdbcTemplate();
    }

    public LiberateResult liberate() {
        LiberateResult result;
        try {
            callbackExecutor.onEvent(Event.BEFORE_LIBERATE);
            result = doLiberate();
        } catch (MigrateDbException e) {
            callbackExecutor.onEvent(Event.AFTER_LIBERATE_ERROR);
            throw e;
        }
        callbackExecutor.onEvent(Event.AFTER_LIBERATE);
        return result;
    }

    private LiberateResult doLiberate() {
        var fromTable = Stream.of(Stream.of(defaultSchema), Arrays.stream(schemas))
                .flatMap(it -> it)
                .map(it -> it.getTable(configuration.getOldTable()))
                .filter(Table::exists)
                .findFirst()
                .orElse(null);
        if (fromTable == null) {
            throw new MigrateDbException("The table " + configuration.getOldTable() +
                    " was not found in any schema");
        }
        if (!schemaHistory.exists()) {
            schemaHistory.create(false);
        } else if (schemaHistory.hasAppliedMigrations()) {
            throw new MigrateDbException("Cannot convert old schema history since target table " +
                    schemaHistory.getTable() + " already has applied migrations");
        }
        var changes = database.getMainConnection().lock(schemaHistory.getTable(),
                () -> convertToMigrateDb(fromTable));
        return CommandResultFactory.createLiberateResult(configuration,
                database,
                schemaHistory.getTable().getSchema().getName(),
                schemaHistory.getTable().getName(),
                changes);
    }

    private List<LiberateAction> convertToMigrateDb(Table<?, ?> fromTable) {
        try {
            var output = new ArrayList<LiberateAction>();
            var oldSchemaHistory = collectOldSchemaHistory(fromTable, output);
            copyToCurrentSchemaHistory(oldSchemaHistory, output);
            return output;
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Failed to convert old schema history", e);
        }
    }

    private List<OldSchemaHistoryRow> collectOldSchemaHistory(Table<?, ?> fromTable,
                                                              List<LiberateAction> output) throws SQLException {
        var oldSchemaHistory = new LinkedList<OldSchemaHistoryRow>();
        var rows = new ArrayList<>(jdbcTemplate.query("select * from " + fromTable, this::readOldSchemaHistoryRow));
        rows.sort(Comparator.comparing(it -> it.installedRank));
        for (var row : rows) {
            if (row.isUndo) {
                output.add(new LiberateAction("skipped_undo_migration",
                        "Skipped undo migration: " + row));
                removeUndoneMigration(oldSchemaHistory, row, output);
                continue;
            } else if (row.isTableMarker) {
                output.add(new LiberateAction("skipped_table_marker",
                        "Skipped table creation marker: " + row));
                continue;
            }
            assert row.type != null;
            var rowType = row.type.name().toLowerCase(Locale.ROOT);
            if (row.type.equals(MigrationType.DELETED)) {
                output.add(new LiberateAction("skipped_" + rowType + "_migration",
                        "Skipped deleted migration: " + row));
                removeDeletedMigration(oldSchemaHistory, row);
                continue;
            }
            if (row.type.equals(MigrationType.SCHEMA)) {
                output.add(new LiberateAction("skipped_" + rowType + "_marker",
                        "Skipped schema creation marker: " + row));
                continue;
            }
            oldSchemaHistory.add(row);
        }
        oldSchemaHistory.sort(Comparator.comparing(it -> it.installedRank));
        return oldSchemaHistory;
    }

    private void removeDeletedMigration(List<OldSchemaHistoryRow> oldSchemaHistory, OldSchemaHistoryRow row) {
        if (row.version != null) {
            oldSchemaHistory.removeIf(it -> row.version.equals(it.version));
        } else {
            oldSchemaHistory.removeIf(it -> it.version == null && Objects.equals(it.description, row.description));
        }
    }

    private void removeUndoneMigration(List<OldSchemaHistoryRow> oldSchemaHistory,
                                       OldSchemaHistoryRow undoRow,
                                       List<LiberateAction> output) {
        assert undoRow.isUndo;
        if (undoRow.success && undoRow.version != null) {
            for (var iter = oldSchemaHistory.listIterator(); iter.hasNext(); ) {
                var next = iter.next();
                if (Objects.equals(undoRow.version, next.version) && undoRow.installedRank > next.installedRank) {
                    iter.remove();
                    output.add(new LiberateAction("skipped_undone_migration",
                            "Skipped undone migration: " + next));
                }
            }
        }
    }

    private void copyToCurrentSchemaHistory(List<OldSchemaHistoryRow> rowsInInsertionOrder, List<LiberateAction> output) throws SQLException {
        for (var row : rowsInInsertionOrder) {
            assert row.type != null;
            String versionStr = row.version == null ? null : row.version.toString();
            String description = row.description;
            if (!database.supportsEmptyMigrationDescription() && "".equals(description)) {
                description = NO_DESCRIPTION_MARKER;
            }
            Object versionObj = versionStr == null ? JdbcNullTypes.StringNull : versionStr;
            Object checksumObj = JdbcNullTypes.StringNull;
            jdbcTemplate.update(database.getInsertStatement(schemaHistory.getTable()),
                    row.installedRank,
                    versionObj,
                    description,
                    row.type.name(),
                    row.script,
                    checksumObj,
                    row.installedBy,
                    row.executionTime,
                    row.success);
            output.add(new LiberateAction("copied_" + row.type.toString().toLowerCase(Locale.ROOT) + "_migration",
                    "Copied to MigrateDB schema history: " + row));
            LOG.info("Copied " + row + " to MigrateDB Schema History");
        }
    }

    private OldSchemaHistoryRow readOldSchemaHistoryRow(ResultSet rs) throws SQLException {
        var reader = new ResultSetReader(rs);
        var installedRank = reader.requireInt("installed_rank");
        var description = reader.requireString("description");
        var version = reader.readString("version").map(Version::parse).orElse(null);
        var typeAsString = reader.requireString("type");
        var script = reader.readString("script").orElse("");
        var installedBy = reader.readString("installed_by").orElse(database.getInstalledBy());
        var executionTime = reader.readInt("execution_time").orElse(0);
        var success = reader.requireBoolean("success");

        var isUndo = typeAsString.contains("UNDO");
        var isTableMarker = typeAsString.equals("TABLE");

        MigrationType type = null;
        try {
            if (!isUndo && !isTableMarker) {
                typeAsString = typeAsString.replace("STATE_SCRIPT", "BASELINE");
                typeAsString = typeAsString.replace("CUSTOM", "JDBC");
                typeAsString = typeAsString.replace("SCRIPT", "SQL");
                type = MigrationType.fromString(typeAsString);
            }
        } catch (IllegalArgumentException e) {
            throw new MigrateDbException("Conversion failed: Unsupported migration type '" + typeAsString + "'");
        }

        return new OldSchemaHistoryRow(
                installedRank,
                description,
                version,
                type,
                isUndo,
                isTableMarker,
                script,
                installedBy,
                executionTime,
                success
        );
    }


    private static class OldSchemaHistoryRow {
        public final int installedRank;
        public final String description;
        public final @Nullable Version version;
        /**
         * Null iff this was an undo migration or a table creation marker.
         */
        public final @Nullable MigrationType type;
        public final boolean isUndo;
        public final boolean isTableMarker;
        public final String script;
        public final String installedBy;
        public final int executionTime;
        public final boolean success;

        private OldSchemaHistoryRow(int installedRank,
                                    String description,
                                    @Nullable Version version,
                                    @Nullable MigrationType type,
                                    boolean isUndo,
                                    boolean isTableMarker,
                                    String script,
                                    String installedBy,
                                    int executionTime,
                                    boolean success) {
            this.installedRank = installedRank;
            this.description = description;
            this.version = version;
            this.type = type;
            this.isUndo = isUndo;
            this.isTableMarker = isTableMarker;
            this.script = script;
            this.installedBy = installedBy;
            this.executionTime = executionTime;
            this.success = success;
        }

        @Override
        public String toString() {
            return type + "{version=" + version + ",description=" + description + ",success=" + success + "}";
        }
    }

    private static final class ResultSetReader {
        private final ResultSet rs;
        private final Map<String, Integer> columnPositions;

        ResultSetReader(ResultSet rs) throws SQLException {
            this.rs = rs;
            var meta = rs.getMetaData();
            var colCount = meta.getColumnCount();
            columnPositions = new HashMap<>(colCount, 2f);
            for (var pos = 1; pos <= colCount; pos++) {
                columnPositions.put(meta.getColumnLabel(pos).toLowerCase(Locale.ROOT), pos);
            }
        }

        boolean requireBoolean(String column) throws SQLException {
            var result = rs.getBoolean(positionOf(column));
            if (rs.wasNull()) {
                throw columnWasNull(column);
            }
            return result;
        }

        Optional<Integer> readInt(String column) throws SQLException {
            var result = rs.getInt(positionOf(column));
            if (rs.wasNull()) {
                return Optional.empty();
            }
            return Optional.of(result);
        }

        int requireInt(String column) throws SQLException {
            return readInt(column).orElseThrow(() -> columnWasNull(column));
        }

        Optional<String> readString(String column) throws SQLException {
            return Optional.ofNullable(rs.getString(positionOf(column)));
        }

        String requireString(String column) throws SQLException {
            return readString(column).orElseThrow(() -> columnWasNull(column));
        }

        private int positionOf(String lowerCaseLabel) {
            Integer position = columnPositions.get(lowerCaseLabel);
            if (position == null) {
                throw new MigrateDbException("Conversion failed: Column '" + lowerCaseLabel +
                        "' not present in old schema history table");
            }
            return position;
        }

        private MigrateDbException columnWasNull(String column) {
            return new MigrateDbException("Conversion failed: Value of '" + column + "' was null");
        }
    }
}
