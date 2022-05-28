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
import migratedb.core.api.Version;
import migratedb.core.api.callback.Event;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.schemahistory.AppliedMigration;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.BaselineResult;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.internal.schemahistory.SchemaHistory;

/**
 * Handles MigrateDB 's baseline command.
 */
public class DbBaseline {
    private static final Log LOG = Log.getLog(DbBaseline.class);

    /**
     * The schema history table.
     */
    private final SchemaHistory schemaHistory;

    /**
     * The version to tag an existing schema with when executing baseline.
     */
    private final Version baselineVersion;

    /**
     * The description to tag an existing schema with when executing baseline.
     */
    private final String baselineDescription;

    /**
     * The callback executor.
     */
    private final CallbackExecutor callbackExecutor;

    /**
     * The result data structure.
     */
    private final BaselineResult baselineResult;

    /**
     * Creates a new DbBaseline.
     *
     * @param schemaHistory       The database schema history table.
     * @param baselineVersion     The version to tag an existing schema with when executing baseline.
     * @param baselineDescription The description to tag an existing schema with when executing baseline.
     * @param callbackExecutor    The callback executor.
     * @param database            Database-specific functionality.
     */
    public DbBaseline(SchemaHistory schemaHistory, Version baselineVersion, String baselineDescription,
                      CallbackExecutor callbackExecutor, Database database) {
        this.schemaHistory = schemaHistory;
        this.baselineVersion = baselineVersion;
        this.baselineDescription = baselineDescription;
        this.callbackExecutor = callbackExecutor;

        baselineResult = CommandResultFactory.createBaselineResult(database.getCatalog());
    }

    /**
     * Baselines the database.
     */
    public BaselineResult baseline() {
        callbackExecutor.onEvent(Event.BEFORE_BASELINE);

        try {
            if (!schemaHistory.exists()) {
                schemaHistory.create(true);
                LOG.info("Successfully baselined schema with version: " + baselineVersion);
                baselineResult.successfullyBaselined = true;
                baselineResult.baselineVersion = baselineVersion.toString();
            } else {
                AppliedMigration baselineMarker = schemaHistory.getBaselineMarker();
                if (baselineMarker != null) {
                    if (baselineVersion.equals(baselineMarker.getVersion())
                            && baselineDescription.equals(baselineMarker.getDescription())) {
                        LOG.info("Schema history table " + schemaHistory + " already initialized with ("
                                + baselineVersion + "," + baselineDescription + "). Skipping.");
                        baselineResult.successfullyBaselined = true;
                        baselineResult.baselineVersion = baselineVersion.toString();
                    } else {
                        throw new MigrateDbException(
                                "Unable to baseline schema history table " + schemaHistory + " with ("
                                        + baselineVersion + "," + baselineDescription
                                        + ") as it has already been baselined with ("
                                        + baselineMarker.getVersion() + "," + baselineMarker.getDescription() + ")");
                    }
                } else {
                    if (schemaHistory.hasSchemasMarker() && baselineVersion.equals(Version.parse("0"))) {
                        throw new MigrateDbException("Unable to baseline schema history table " + schemaHistory +
                                " with version 0 as this version was used for schema creation");
                    }

                    if (schemaHistory.hasAppliedMigrations()) {
                        throw new MigrateDbException("Unable to baseline schema history table " + schemaHistory +
                                " as it already contains migrations");
                    }

                    if (schemaHistory.allAppliedMigrations().isEmpty()) {
                        throw new MigrateDbException("Unable to baseline schema history table " + schemaHistory +
                                " as it already exists, and is empty.\n" +
                                "Delete the schema history table with the clean command, and run" +
                                " baseline again.");
                    }

                    throw new MigrateDbException("Unable to baseline schema history table " + schemaHistory +
                            " as it already contains migrations.\n" +
                            "Delete the schema history table with the clean command, and run " +
                            "baseline again.");
                }
            }
        } catch (MigrateDbException e) {
            callbackExecutor.onEvent(Event.AFTER_BASELINE_ERROR);
            baselineResult.successfullyBaselined = false;
            throw e;
        }

        callbackExecutor.onEvent(Event.AFTER_BASELINE);

        return baselineResult;
    }
}
