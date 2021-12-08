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
package migratedb.core.internal.database.redshift;

import java.sql.SQLException;
import migratedb.core.api.MigrateDbException;
import migratedb.core.internal.database.base.Connection;
import migratedb.core.internal.database.base.Schema;
import migratedb.core.internal.exception.MigrateDbSqlException;
import migratedb.core.internal.util.StringUtils;

/**
 * Redshift connection.
 */
public class RedshiftConnection extends Connection<RedshiftDatabase> {
    RedshiftConnection(RedshiftDatabase database, java.sql.Connection connection) {
        super(database, connection);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return jdbcTemplate.queryForString("SHOW search_path");
    }

    @Override
    public void changeCurrentSchemaTo(Schema schema) {
        try {
            if (schema.getName().equals(originalSchemaNameOrSearchPath) || originalSchemaNameOrSearchPath.startsWith(
                schema.getName() + ",") || !schema.exists()) {
                return;
            }

            if (StringUtils.hasText(originalSchemaNameOrSearchPath) &&
                !"unset".equals(originalSchemaNameOrSearchPath)) {
                doChangeCurrentSchemaOrSearchPathTo(schema.toString() + "," + originalSchemaNameOrSearchPath);
            } else {
                doChangeCurrentSchemaOrSearchPathTo(schema.toString());
            }
        } catch (SQLException e) {
            throw new MigrateDbSqlException("Error setting current schema to " + schema, e);
        }
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        if ("unset".equals(schema)) {
            schema = "";
        }
        jdbcTemplate.execute("SELECT set_config('search_path', ?, false)", schema);
    }

    @Override
    public Schema doGetCurrentSchema() throws SQLException {
        String currentSchema = jdbcTemplate.queryForString("SELECT current_schema()");
        String searchPath = getCurrentSchemaNameOrSearchPath();

        if (!StringUtils.hasText(currentSchema) && !StringUtils.hasText(searchPath)) {
            throw new MigrateDbException("Unable to determine current schema as search_path is empty. " +
                                         "Set the current schema in currentSchema parameter of the JDBC URL or in " +
                                         "MigrateDb's schemas property.");
        }

        String schema = StringUtils.hasText(currentSchema) ? currentSchema : searchPath;

        return getSchema(schema);
    }

    @Override
    public Schema getSchema(String name) {
        return new RedshiftSchema(jdbcTemplate, database, name);
    }
}
