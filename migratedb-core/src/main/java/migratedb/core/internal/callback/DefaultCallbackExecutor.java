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
package migratedb.core.internal.callback;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.callback.Context;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.database.base.Connection;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.OperationResult;
import migratedb.core.internal.jdbc.ExecutionTemplateFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Executes the callbacks for a specific event.
 */
public class DefaultCallbackExecutor implements CallbackExecutor {
    private static final Log LOG = Log.getLog(DefaultCallbackExecutor.class);

    private final Configuration configuration;
    private final Database<?> database;
    private final Schema<?, ?> schema;
    private final List<Callback> callbacks;
    private MigrationInfo migrationInfo;

    /**
     * Creates a new callback executor.
     *
     * @param configuration The configuration.
     * @param database      The database.
     * @param schema        The current schema to use for the connection.
     * @param callbacks     The callbacks to execute.
     */
    public DefaultCallbackExecutor(Configuration configuration, Database<?> database, Schema<?, ?> schema,
                                   Collection<Callback> callbacks) {
        this.configuration = configuration;
        this.database = database;
        this.schema = schema;

        this.callbacks = new ArrayList<>(callbacks);
        this.callbacks.sort(Comparator.comparing(Callback::getCallbackName));
    }

    @Override
    public void onEvent(Event event) {
        execute(event, database.getMainConnection());
    }

    @Override
    public void onMigrateEvent(Event event) {
        execute(event, database.getMigrationConnection());
    }

    @Override
    public void setMigrationInfo(MigrationInfo migrationInfo) {
        this.migrationInfo = migrationInfo;
    }

    @Override
    public void onEachMigrateOrUndoEvent(Event event) {
        Context context = new SimpleContext(configuration, database.getMigrationConnection(), migrationInfo, null);
        for (Callback callback : callbacks) {
            if (callback.supports(event, context)) {
                callback.handle(event, context);
            }
        }
    }

    @Override
    public void onOperationFinishEvent(Event event, OperationResult operationResult) {
        Context context = new SimpleContext(configuration,
                                            database.getMigrationConnection(),
                                            migrationInfo,
                                            operationResult);
        for (Callback callback : callbacks) {
            if (callback.supports(event, context)) {
                callback.handle(event, context);
            }
        }
    }

    private void execute(Event event, Connection<?> connection) {
        Context context = new SimpleContext(configuration, connection, null, null);
        for (Callback callback : callbacks) {
            if (callback.supports(event, context)) {
                if (callback.canHandleInTransaction(event, context)) {
                    ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(
                            (Callable<Void>) () -> {
                                execute(connection, callback, event, context);
                                return null;
                            });
                } else {
                    execute(connection, callback, event, context);
                }
            }
        }
    }

    private void execute(Connection<?> connection, Callback callback, Event event, Context context) {
        connection.restoreOriginalState();
        connection.changeCurrentSchemaTo(schema);
        handleEvent(callback, event, context);
    }

    private void handleEvent(Callback callback, Event event, Context context) {
        try {
            callback.handle(event, context);
        } catch (RuntimeException e) {
            throw new MigrateDbException("Error while executing " + event.getId() + " callback: " + e.getMessage(), e);
        }
    }
}
