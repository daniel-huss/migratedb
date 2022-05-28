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

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SchemaHistoryFactory {
    private static final Log LOG = Log.getLog(SchemaHistoryFactory.class);

    private SchemaHistoryFactory() {
        // Prevent instantiation
    }

    public static SchemaHistory getSchemaHistory(Configuration configuration,
                                                 SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                                 SqlScriptFactory sqlScriptFactory,
                                                 Database<?> database,
                                                 Schema<?, ?> schema,
                                                 StatementInterceptor statementInterceptor) {
        var table = schema.getTable(configuration.getTable());

        return new JdbcTableSchemaHistory(sqlScriptExecutorFactory,
                sqlScriptFactory,
                database,
                table);
    }

    public static final class SchemasWithDefault {
        public final List<Schema<?, ?>> all;
        public final Schema<?, ?> defaultSchema;

        public SchemasWithDefault(List<Schema<?, ?>> schemas, Schema<?, ?> defaultSchema) {
            this.all = schemas;
            this.defaultSchema = defaultSchema;
        }
    }

    public static SchemasWithDefault scanSchemas(Configuration configuration, Database<?> database) {
        String[] schemaNames = configuration.getSchemas();

        String defaultSchemaName = configuration.getDefaultSchema();

        LOG.debug("Schemas: " + StringUtils.arrayToCommaDelimitedString(schemaNames));
        LOG.debug("Default schema: " + defaultSchemaName);

        List<Schema<?, ?>> schemas = new ArrayList<>();
        for (String schemaName : schemaNames) {
            schemas.add(database.getMainConnection().getSchema(schemaName));
        }

        if (defaultSchemaName == null) {
            if (schemaNames.length == 0) {
                Schema<?, ?> currentSchema = database.getMainConnection().getCurrentSchema();
                if (currentSchema == null || currentSchema.getName() == null) {
                    throw new MigrateDbException(
                            "Unable to determine schema for the schema history table. Set a default schema for the " +
                                    "connection or specify one using the 'defaultSchema' property");
                }
                defaultSchemaName = currentSchema.getName();
            } else {
                defaultSchemaName = schemaNames[0];
            }
        }

        Schema<?, ?> defaultSchema = database.getMainConnection().getSchema(defaultSchemaName);
        if (!schemas.contains(defaultSchema)) {
            schemas.add(0, defaultSchema);
        }

        return new SchemasWithDefault(schemas, defaultSchema);
    }
}
