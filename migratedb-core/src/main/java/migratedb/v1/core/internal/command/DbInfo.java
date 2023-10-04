/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
import migratedb.v1.core.api.MigrationInfoService;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.internal.info.MigrationInfoServiceImpl;
import migratedb.v1.core.internal.info.ValidationContext;
import migratedb.v1.core.internal.info.ValidationMatch;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;

import java.util.EnumSet;

public class DbInfo {
    private final MigrationResolver migrationResolver;
    private final SchemaHistory schemaHistory;
    private final Configuration configuration;
    private final Database database;
    private final CallbackExecutor callbackExecutor;
    private final Schema[] schemas;

    public DbInfo(MigrationResolver migrationResolver,
                  SchemaHistory schemaHistory,
                  Configuration configuration,
                  Database database,
                  CallbackExecutor callbackExecutor,
                  Schema[] schemas) {

        this.migrationResolver = migrationResolver;
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.database = database;
        this.callbackExecutor = callbackExecutor;
        this.schemas = schemas;
    }

    public MigrationInfoService info() {
        callbackExecutor.onEvent(Event.BEFORE_INFO);

        MigrationInfoServiceImpl migrationInfoService;
        try {
            var allowedMatches = EnumSet.allOf(ValidationMatch.class);
            if (!configuration.isOutOfOrder()) {
                allowedMatches.remove(ValidationMatch.OUT_OF_ORDER);
            }
            migrationInfoService = new MigrationInfoServiceImpl(migrationResolver,
                                                                schemaHistory,
                                                                database,
                                                                configuration,
                                                                configuration.getTarget(),
                                                                configuration.getCherryPick(),
                                                                new ValidationContext(allowedMatches));
            migrationInfoService.refresh();
            migrationInfoService.setAllSchemasEmpty(schemas);
        } catch (MigrateDbException e) {
            callbackExecutor.onEvent(Event.AFTER_INFO_ERROR);
            throw e;
        }

        callbackExecutor.onEvent(Event.AFTER_INFO);

        return migrationInfoService;
    }
}
