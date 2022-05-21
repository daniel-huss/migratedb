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
package migratedb.core.internal.schemahistory;

import migratedb.core.api.*;
import migratedb.core.api.internal.database.base.Connection;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.api.internal.jdbc.RowMapper;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.api.output.RepairResult;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.jdbc.JdbcNullTypes;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

import static migratedb.core.internal.jdbc.ExecutionTemplateFactory.createExecutionTemplate;

/**
 * Supports reading and writing to the schema history table.
 */
class JdbcTableSchemaHistory extends SchemaHistory {
    private static final Log LOG = Log.getLog(JdbcTableSchemaHistory.class);

    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;
    private final SqlScriptFactory sqlScriptFactory;

    /**
     * The database to use.
     */
    private final Database database;

    /**
     * Connection with access to the database.
     */
    private final Connection<?> connection;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Applied migration cache.
     */
    private final LinkedList<AppliedMigration> cache = new LinkedList<>();

    /**
     * Creates a new instance of the schema history table support.
     *
     * @param database The database to use.
     * @param table    The schema history table used by MigrateDb.
     */
    JdbcTableSchemaHistory(SqlScriptExecutorFactory sqlScriptExecutorFactory, SqlScriptFactory sqlScriptFactory,
                           Database database, Table table) {
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
        this.sqlScriptFactory = sqlScriptFactory;
        this.table = table;
        this.database = database;
        this.connection = database.getMainConnection();
        this.jdbcTemplate = connection.getJdbcTemplate();
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @Override
    public boolean exists() {
        connection.restoreOriginalState();

        return table.exists();
    }

    @Override
    public void create(boolean baseline) {
        connection.lock(table, () -> {
            int retries = 0;
            while (!exists()) {
                if (retries == 0) {
                    LOG.info(
                        "Creating Schema History table " + table + (baseline ? " with baseline" : "") + " ...");
                }
                try {
                    createExecutionTemplate(connection.getJdbcConnection(),
                                            database).execute(() -> {
                        sqlScriptExecutorFactory.createSqlScriptExecutor(connection.getJdbcConnection(),
                                                                         false,
                                                                         true)
                                                .execute(database.getCreateScript(sqlScriptFactory,
                                                                                  table,
                                                                                  baseline));
                        LOG.debug("Created Schema History table " + table + (baseline ? " with baseline" : ""));
                        return null;
                    });
                } catch (MigrateDbException e) {
                    if (++retries >= 10) {
                        throw e;
                    }
                    try {
                        LOG.debug("Schema History table creation failed. Retrying in 1 sec ...");
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // Ignore
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return null;
        });
    }

    @Override
    public <T> T lock(Callable<T> callable) {
        connection.restoreOriginalState();

        return connection.lock(table, callable);
    }

    @Override
    public void addAppliedMigration(int installedRank,
                                    Version version,
                                    String description,
                                    MigrationType type,
                                    String script,
                                    @Nullable Checksum checksum,
                                    int executionTime,
                                    boolean success) {
        boolean tableIsLocked = false;
        connection.restoreOriginalState();

        // Lock again for databases with no clean DDL transactions like Oracle
        // to prevent implicit commits from triggering deadlocks
        // in highly concurrent environments
        if (!database.supportsDdlTransactions()) {
            table.lock();
            tableIsLocked = true;
        }

        try {
            String versionStr = version == null ? null : version.toString();

            if (!database.supportsEmptyMigrationDescription() && "".equals(description)) {
                description = NO_DESCRIPTION_MARKER;
            }

            Object versionObj = versionStr == null ? JdbcNullTypes.StringNull : versionStr;
            Object checksumObj = checksum == null ? JdbcNullTypes.StringNull : checksum.toString();

            jdbcTemplate.update(database.getInsertStatement(table),
                                installedRank,
                                versionObj,
                                description,
                                type.name(),
                                script,
                                checksumObj,
                                database.getInstalledBy(),
                                executionTime,
                                success);

            LOG.debug("Schema History table " + table + " successfully updated to reflect changes");
        } catch (SQLException e) {
            throw new MigrateDbSqlException(
                "Unable to insert row for version '" + version + "' in Schema History table " + table, e);
        } finally {
            if (tableIsLocked) {
                table.unlock();
            }
        }
    }

    @Override
    public List<AppliedMigration> allAppliedMigrations() {
        if (!exists()) {
            return new ArrayList<>();
        }

        refreshCache();
        return cache;
    }

    private void refreshCache() {
        int maxCachedInstalledRank = cache.isEmpty() ? -1 : cache.getLast().getInstalledRank();

        String query = database.getSelectStatement(table);

        try {
            cache.addAll(jdbcTemplate.query(query, new RowMapper<AppliedMigration>() {
                @Override
                public AppliedMigration mapRow(ResultSet rs) throws SQLException {
                    // Construct a map of lower-cased column names to ordinals. This is useful for databases that
                    // upper-case them - eg Snowflake with QUOTED-IDENTIFIERS-IGNORE-CASE turned on
                    HashMap<String, Integer> columnOrdinalMap = constructColumnOrdinalMap(rs);

                    String checksum = rs.getString(columnOrdinalMap.get("checksum"));
                    if (rs.wasNull()) {
                        checksum = null;
                    }

                    // Convert legacy types to their modern equivalent to avoid validation errors
                    String type = rs.getString(columnOrdinalMap.get("type"));
                    if ("SPRING_JDBC".equals(type)) {
                        type = "JDBC";
                    }

                    return new AppliedMigration(
                        rs.getInt(columnOrdinalMap.get("installed_rank")),
                        rs.getString(columnOrdinalMap.get("version")) != null
                        ? Version.parse(rs.getString(columnOrdinalMap.get("version"))) : null,
                        rs.getString(columnOrdinalMap.get("description")),
                        MigrationType.fromString(type),
                        rs.getString(columnOrdinalMap.get("script")),
                        checksum == null ? null : Checksum.parse(checksum),
                        rs.getTimestamp(columnOrdinalMap.get("installed_on")),
                        rs.getString(columnOrdinalMap.get("installed_by")),
                        rs.getInt(columnOrdinalMap.get("execution_time")),
                        rs.getBoolean(columnOrdinalMap.get("success"))
                    );
                }
            }, maxCachedInstalledRank));
        } catch (SQLException e) {
            throw new MigrateDbSqlException(
                "Error while retrieving the list of applied migrations from Schema History table "
                + table,
                e);
        }
    }

    private HashMap<String, Integer> constructColumnOrdinalMap(ResultSet rs) throws SQLException {
        HashMap<String, Integer> columnOrdinalMap = new HashMap<>();
        ResultSetMetaData metadata = rs.getMetaData();

        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            // Careful - column ordinals in JDBC start at 1
            String columnNameLower = metadata.getColumnName(i).toLowerCase(Locale.ROOT);
            columnOrdinalMap.put(columnNameLower, i);
        }
        return columnOrdinalMap;
    }

    @Override
    public boolean removeFailedMigrations(RepairResult repairResult, MigrationPattern[] migrationPatternFilter) {
        if (!exists()) {
            LOG.info("Repair of failed migration in Schema History table " + table +
                     " not necessary as table doesn't exist.");
            return false;
        }

        List<AppliedMigration> appliedMigrations = filterMigrations(allAppliedMigrations(), migrationPatternFilter);

        boolean failed = appliedMigrations.stream().anyMatch(am -> !am.isSuccess());
        if (!failed) {
            LOG.info("Repair of failed migration in Schema History table " + table +
                     " not necessary. No failed migration detected.");
            return false;
        }

        try {
            appliedMigrations.stream()
                             .filter(am -> !am.isSuccess())
                             .forEach(am -> repairResult.migrationsRemoved.add(CommandResultFactory.createRepairOutput(
                                 am)));

            for (AppliedMigration appliedMigration : appliedMigrations) {
                jdbcTemplate.execute("DELETE FROM " + table +
                                     " WHERE " + database.quote("success") + " = " + database.getBooleanFalse() +
                                     " AND " +
                                     (appliedMigration.getVersion() != null ?
                                      database.quote("version") + " = '" + appliedMigration.getVersion() +
                                      "'" :
                                      database.quote("description") + " = '" + appliedMigration.getDescription() +
                                      "'"));
            }

            clearCache();
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to repair Schema History table " + table, e);
        }

        return true;
    }

    private List<AppliedMigration> filterMigrations(List<AppliedMigration> appliedMigrations,
                                                    MigrationPattern[] migrationPatternFilter) {
        if (migrationPatternFilter == null) {
            return appliedMigrations;
        }

        Set<AppliedMigration> filteredList = new HashSet<>();

        for (AppliedMigration appliedMigration : appliedMigrations) {
            for (MigrationPattern migrationPattern : migrationPatternFilter) {
                if (migrationPattern.matches(appliedMigration.getVersion(), appliedMigration.getDescription())) {
                    filteredList.add(appliedMigration);
                }
            }
        }

        return new ArrayList<>(filteredList);
    }

    @Override
    public void update(AppliedMigration appliedMigration, ResolvedMigration resolvedMigration) {
        connection.restoreOriginalState();

        clearCache();

        Version version = appliedMigration.getVersion();

        String description = resolvedMigration.getDescription();
        Checksum checksum = resolvedMigration.getChecksum();
        MigrationType type = appliedMigration.getType().isExclusiveToAppliedMigrations()
                             ? appliedMigration.getType()
                             : resolvedMigration.getType();

        LOG.info("Repairing Schema History table for version " + version
                 + " (Description: " + description + ", Type: " + type + ", Checksum: " + checksum + ")  ...");

        if (!database.supportsEmptyMigrationDescription() && "".equals(description)) {
            description = NO_DESCRIPTION_MARKER;
        }

        Object checksumObj = checksum == null ? JdbcNullTypes.StringNull : checksum.toString();

        try {
            jdbcTemplate.update("UPDATE " + table
                                + " SET "
                                + database.quote("description") + "=? , "
                                + database.quote("type") + "=? , "
                                + database.quote("checksum") + "=?"
                                + " WHERE " + database.quote("installed_rank") + "=?",
                                description, type.name(), checksumObj, appliedMigration.getInstalledRank());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to repair Schema History table " + table
                                            + " for version " + version, e);
        }
    }

    @Override
    public void delete(AppliedMigration appliedMigration) {
        connection.restoreOriginalState();

        clearCache();

        Version version = appliedMigration.getVersion();

        if (version == null) {
            LOG.info("Repairing Schema History table for description \"" + appliedMigration.getDescription() +
                     "\" (Marking as DELETED)  ...");
        } else {
            LOG.info("Repairing Schema History table for version \"" + version + "\" (Marking as DELETED)  ...");
        }

        try {
            jdbcTemplate.update("UPDATE " + table
                            + " SET "
                            + database.quote("type") + "=?  "
                            + " WHERE " + database.quote("installed_rank") + "=?",
                                "DELETED", appliedMigration.getInstalledRank());
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Unable to repair Schema History table " + table
                                            + " for version " + version, e);
        }
    }
}
