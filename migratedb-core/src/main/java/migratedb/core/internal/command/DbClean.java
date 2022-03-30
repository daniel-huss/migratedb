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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Connection;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.CleanResult;
import migratedb.core.api.output.CommandResultFactory;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.jdbc.ExecutionTemplateFactory;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.DateTimeUtils;
import migratedb.core.internal.util.StopWatch;

public class DbClean {
    private static final Log LOG = Log.getLog(DbClean.class);
    private final SchemaHistory schemaHistory;
    protected final Schema defaultSchema;
    protected final Schema[] schemas;
    protected final Connection connection;
    protected final Database database;
    protected final CallbackExecutor callbackExecutor;
    protected final Configuration configuration;

    public DbClean(Database database, SchemaHistory schemaHistory, Schema defaultSchema, Schema[] schemas,
                   CallbackExecutor callbackExecutor, Configuration configuration) {
        this.schemaHistory = schemaHistory;
        this.defaultSchema = defaultSchema;
        this.schemas = schemas;
        this.connection = database.getMainConnection();
        this.database = database;
        this.callbackExecutor = callbackExecutor;
        this.configuration = configuration;
    }

    public CleanResult clean() throws MigrateDbException {
        if (configuration.isCleanDisabled()) {
            throw new MigrateDbException(
                "Unable to execute clean as it has been disabled");
        }

        callbackExecutor.onEvent(Event.BEFORE_CLEAN);

        CleanResult cleanResult = CommandResultFactory.createCleanResult(database.getCatalog());
        clean(cleanResult);

        callbackExecutor.onEvent(Event.AFTER_CLEAN);
        schemaHistory.clearCache();

        return cleanResult;
    }

    protected void clean(CleanResult cleanResult) {
        clean(defaultSchema, schemas, cleanResult);
    }

    protected void clean(Schema defaultSchema, Schema[] schemas, CleanResult cleanResult) {
        try {
            connection.changeCurrentSchemaTo(defaultSchema);

            List<String> dropSchemas = new ArrayList<>();
            try {
                dropSchemas = schemaHistory.getSchemasCreatedByMigrateDb();
            } catch (Exception e) {
                LOG.error("Error while checking whether the schemas should be dropped. Schemas will not be dropped", e);
            }

            clean(schemas, cleanResult, dropSchemas);
        } catch (MigrateDbException e) {
            callbackExecutor.onEvent(Event.AFTER_CLEAN_ERROR);
            throw e;
        }
    }

    protected void clean(Schema[] schemas, CleanResult cleanResult, List<String> dropSchemas) {
        dropDatabaseObjectsPreSchemas();

        List<Schema> schemaList = new LinkedList<>(Arrays.asList(schemas));
        for (int i = 0; i < schemaList.size(); ) {
            Schema schema = schemaList.get(i);
            if (!schema.exists()) {
                String unknownSchemaWarning = "Unable to clean unknown schema: " + schema;
                cleanResult.warnings.add(unknownSchemaWarning);
                LOG.warn(unknownSchemaWarning);
                schemaList.remove(i);
            } else {
                i++;
            }
        }
        cleanSchemas(schemaList.toArray(new Schema[0]), dropSchemas, cleanResult);
        Collections.reverse(schemaList);
        cleanSchemas(schemaList.toArray(new Schema[0]), dropSchemas, null);

        dropDatabaseObjectsPostSchemas(schemas);

        for (Schema schema : schemas) {
            if (dropSchemas.contains(schema.getName())) {
                dropSchema(schema, cleanResult);
            }
        }
    }

    /**
     * Drops database-level objects that need to be cleaned prior to schema-level objects.
     */
    private void dropDatabaseObjectsPreSchemas() {
        LOG.debug("Dropping pre-schema database level objects...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(() -> {
                database.cleanPreSchemas();
                return null;
            });
            stopWatch.stop();
            LOG.info(String.format("Successfully dropped pre-schema database level objects (execution time %s)",
                                   DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));
        } catch (MigrateDbSqlException e) {
            LOG.debug(e.getMessage());
            LOG.warn("Unable to drop pre-schema database level objects");
        }
    }

    /**
     * Drops database-level objects that need to be cleaned after all schema-level objects.
     */
    private void dropDatabaseObjectsPostSchemas(Schema[] schemas) {
        LOG.debug("Dropping post-schema database level objects...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(() -> {
                database.cleanPostSchemas(schemas);
                return null;
            });
            stopWatch.stop();
            LOG.info(String.format("Successfully dropped post-schema database level objects (execution time %s)",
                                   DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));
        } catch (MigrateDbSqlException e) {
            LOG.debug(e.getMessage());
            LOG.warn("Unable to drop post-schema database level objects");
        }
    }

    private void dropSchema(Schema schema, CleanResult cleanResult) {
        LOG.debug("Dropping schema " + schema + "...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(() -> {
                schema.drop();
                return null;
            });

            cleanResult.schemasDropped.add(schema.getName());

            stopWatch.stop();
            LOG.info(String.format("Successfully dropped schema %s (execution time %s)",
                                   schema, DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));
        } catch (MigrateDbSqlException e) {
            LOG.debug(e.getMessage());
            LOG.warn("Unable to drop schema " + schema + ". It was cleaned instead.");
            cleanResult.schemasCleaned.add(schema.getName());
        }
    }

    private void cleanSchemas(Schema[] schemas, List<String> dropSchemas, CleanResult cleanResult) {
        for (Schema schema : schemas) {
            if (dropSchemas.contains(schema.getName())) {
                try {
                    cleanSchema(schema);
                } catch (MigrateDbException ignored) {
                }
            } else {
                cleanSchema(schema);
                if (cleanResult != null) {
                    cleanResult.schemasCleaned.add(schema.getName());
                }
            }
        }
    }

    private void cleanSchema(Schema schema) {
        LOG.debug("Cleaning schema " + schema + "...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        doCleanSchema(schema);
        stopWatch.stop();
        LOG.info(String.format("Successfully cleaned schema %s (execution time %s)",
                               schema, DateTimeUtils.formatDuration(stopWatch.getTotalTimeMillis())));
    }

    protected void doCleanSchema(Schema schema) {
        ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(() -> {
            schema.clean();
            return null;
        });
    }
}
