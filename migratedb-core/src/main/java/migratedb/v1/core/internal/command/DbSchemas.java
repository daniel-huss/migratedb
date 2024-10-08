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
package migratedb.v1.core.internal.command;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.jdbc.ExecutionTemplateFactory;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles MigrateDB 's automatic schema creation.
 */
public class DbSchemas {
    private static final Log LOG = Log.getLog(DbSchemas.class);

    /**
     * The database session to use for accessing the schema history table.
     */
    private final Session session;

    /**
     * The schemas managed by MigrateDB.
     */
    private final Schema[] schemas;

    /**
     * The schema history table.
     */
    private final SchemaHistory schemaHistory;

    /**
     * The database.
     */
    private final Database database;

    /**
     * The callback executor.
     */
    private final CallbackExecutor callbackExecutor;

    /**
     * Creates a new DbSchemas.
     *
     * @param database      The database to use.
     * @param schemas       The schemas managed by MigrateDB.
     * @param schemaHistory The schema history table.
     */
    public DbSchemas(Database database, Schema[] schemas, SchemaHistory schemaHistory,
                     CallbackExecutor callbackExecutor) {
        this.database = database;
        this.session = database.getMainSession();
        this.schemas = schemas;
        this.schemaHistory = schemaHistory;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Creates the schemas.
     *
     * @param baseline Whether to include the creation of a baseline marker.
     */
    public void create(boolean baseline) {
        callbackExecutor.onEvent(Event.CREATE_SCHEMA);
        int retries = 0;
        while (true) {
            try {
                ExecutionTemplateFactory.createExecutionTemplate(session.getJdbcConnection(), database)
                                        .execute(() -> {
                                            List<Schema> createdSchemas = new ArrayList<>();
                                            for (var schema : schemas) {
                                                if (!schema.exists()) {
                                                    if (schema.getName() == null) {
                                                        throw new MigrateDbException(
                                                                "Unable to determine schema for the schema history table." +
                                                                " Set a default schema for the connection or specify one " +
                                                                "using the defaultSchema property!");
                                                    }
                                                    LOG.debug("Creating schema: " + schema);
                                                    schema.create();
                                                    createdSchemas.add(schema);
                                                } else {
                                                    LOG.debug("Skipping creation of existing schema: " + schema);
                                                }
                                            }

                                            if (!createdSchemas.isEmpty()) {
                                                schemaHistory.create(baseline);
                                                schemaHistory.addSchemasMarker(createdSchemas.toArray(new Schema[0]));
                                            }

                                            return null;
                                        });
                return;
            } catch (RuntimeException e) {
                if (++retries >= 10) {
                    throw e;
                }
                try {
                    LOG.debug("Schema creation failed. Retrying in 1 sec ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new MigrateDbException("Interrupted");
                }
            }
        }
    }
}
