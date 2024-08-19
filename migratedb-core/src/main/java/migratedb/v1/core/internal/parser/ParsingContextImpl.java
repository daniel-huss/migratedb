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
package migratedb.v1.core.internal.parser;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.resource.ResourceName;
import migratedb.v1.core.api.logging.Log;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ParsingContextImpl implements ParsingContext {
    private static final Log LOG = Log.getLog(ParsingContextImpl.class);

    private static final String DEFAULT_SCHEMA_PLACEHOLDER = "migratedb:defaultSchema";
    private static final String USER_PLACEHOLDER = "migratedb:user";
    private static final String DATABASE_PLACEHOLDER = "migratedb:database";
    private static final String TIMESTAMP_PLACEHOLDER = "migratedb:timestamp";
    private static final String FILENAME_PLACEHOLDER = "migratedb:filename";
    private static final String WORKING_DIRECTORY_PLACEHOLDER = "migratedb:workingDirectory";
    private static final String TABLE_PLACEHOLDER = "migratedb:table";

    private final Map<String, String> placeholders = new HashMap<>();
    private Database database;

    @Override
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    private void setDatabase(Database database) {
        this.database = database;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    public void populate(Database database, Configuration configuration) {
        setDatabase(database);

        var defaultSchemaName = configuration.getDefaultSchema();
        var schemaNames = configuration.getSchemas();

        var currentSchema = getCurrentSchema(database);
        var catalog = database.getCatalog();
        var currentUser = getCurrentUser(database);

        if (defaultSchemaName == null) {
            if (!schemaNames.isEmpty()) {
                defaultSchemaName = schemaNames.get(0);
            } else {
                assert currentSchema != null;
                defaultSchemaName = currentSchema.getName();
            }
        }

        if (defaultSchemaName != null) {
            placeholders.put(DEFAULT_SCHEMA_PLACEHOLDER, defaultSchemaName);
        }

        if (catalog != null) {
            placeholders.put(DATABASE_PLACEHOLDER, catalog);
        }

        placeholders.put(USER_PLACEHOLDER, currentUser);
        placeholders.put(TIMESTAMP_PLACEHOLDER, Instant.now().toString());
        placeholders.put(WORKING_DIRECTORY_PLACEHOLDER, System.getProperty("user.dir"));
        placeholders.put(TABLE_PLACEHOLDER, configuration.getTable());
    }

    @Override
    public void updateFilenamePlaceholder(ResourceName resourceName) {
        if (resourceName.isValid()) {
            placeholders.put(FILENAME_PLACEHOLDER, resourceName.getFilename());
        } else {
            placeholders.remove(FILENAME_PLACEHOLDER);
        }
    }

    private Schema getCurrentSchema(Database database) {
        try {
            return database.getMainSession().getCurrentSchema();
        } catch (MigrateDbException e) {
            LOG.debug("Could not get schema for " + DEFAULT_SCHEMA_PLACEHOLDER + " placeholder.");
            return null;
        }
    }

    private String getCurrentUser(Database database) {
        try {
            return database.getCurrentUser();
        } catch (MigrateDbException e) {
            LOG.debug("Could not get user for " + USER_PLACEHOLDER + " placeholder.");
            return null;
        }
    }
}
