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

import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.callback.NoopCallbackExecutor;
import migratedb.core.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.internal.jdbc.StatementInterceptor;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.sqlscript.SqlScriptFactory;
import migratedb.core.internal.util.StringUtils;

public class SchemaHistoryFactory {
    private static final Log LOG = Log.getLog(SchemaHistoryFactory.class);

    private SchemaHistoryFactory() {
        // Prevent instantiation
    }

    public static SchemaHistory getSchemaHistory(Configuration configuration,
                                                 SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                                 SqlScriptFactory sqlScriptFactory,
                                                 Database database, Schema schema,
                                                 StatementInterceptor statementInterceptor) {
        Table table = schema.getTable(configuration.getTable());
        JdbcTableSchemaHistory jdbcTableSchemaHistory =
            new JdbcTableSchemaHistory(sqlScriptExecutorFactory, sqlScriptFactory, database, table);

        return jdbcTableSchemaHistory;
    }

    public static SchemaHistory getSchemaHistory(Configuration configuration) {
        JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory(
            configuration.getDataSource(),
            configuration,
            null);

        DatabaseType databaseType = jdbcConnectionFactory.getDatabaseType();
        ParsingContext parsingContext = new ParsingContext();
        SqlScriptFactory sqlScriptFactory = databaseType.createSqlScriptFactory(configuration, parsingContext);

        SqlScriptExecutorFactory noCallbackSqlScriptExecutorFactory = databaseType.createSqlScriptExecutorFactory(
            jdbcConnectionFactory,
            NoopCallbackExecutor.INSTANCE,
            null);

        Database database = databaseType.createDatabase(
            configuration,
            true,
            jdbcConnectionFactory,
            null);

        var schemas = scanSchemas(configuration, database);

        SchemaHistory schemaHistory = SchemaHistoryFactory.getSchemaHistory(
            configuration,
            noCallbackSqlScriptExecutorFactory,
            sqlScriptFactory,
            database,
            schemas.defaultSchema,
            null);

        return schemaHistory;
    }

    public static final class SchemasWithDefault {
        public final List<Schema> all;
        public final Schema defaultSchema;

        public SchemasWithDefault(List<Schema> schemas, Schema defaultSchema) {
            this.all = schemas;
            this.defaultSchema = defaultSchema;
        }
    }

    public static SchemasWithDefault scanSchemas(Configuration configuration, Database database) {
        String defaultSchemaName = configuration.getDefaultSchema();
        String[] schemaNames = configuration.getSchemas();

        if (!isDefaultSchemaValid(defaultSchemaName, schemaNames)) {
            throw new MigrateDbException(
                "The defaultSchema property is specified but is not a member of the schemas property");
        }

        LOG.debug("Schemas: " + StringUtils.arrayToCommaDelimitedString(schemaNames));
        LOG.debug("Default schema: " + defaultSchemaName);

        List<Schema> schemas = new ArrayList<>();

        if (schemaNames.length == 0) {
            Schema currentSchema = database.getMainConnection().getCurrentSchema();
            if (currentSchema == null) {
                throw new MigrateDbException("Unable to determine schema for the schema history table." +
                                             " Set a default schema for the connection or specify one using the " +
                                             "defaultSchema property!");
            }
            schemas.add(currentSchema);
        } else {
            for (String schemaName : schemaNames) {
                schemas.add(database.getMainConnection().getSchema(schemaName));
            }
            if (defaultSchemaName == null) {
                defaultSchemaName = schemaNames[0];
            }
        }

        var defaultSchema = (defaultSchemaName != null)
                            ? database.getMainConnection().getSchema(defaultSchemaName)
                            : database.getMainConnection().getCurrentSchema();

        return new SchemasWithDefault(schemas, defaultSchema);
    }

    private static boolean isDefaultSchemaValid(String defaultSchema, String[] schemas) {
        // No default schema specified
        if (defaultSchema == null) {
            return true;
        }
        // Default schema is one of those MigrateDb is managing
        for (String schema : schemas) {
            if (defaultSchema.equals(schema)) {
                return true;
            }
        }
        return false;
    }
}
