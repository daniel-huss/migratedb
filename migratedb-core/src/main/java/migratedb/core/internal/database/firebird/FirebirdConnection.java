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
package migratedb.core.internal.database.firebird;

import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.internal.database.base.BaseConnection;

import java.sql.SQLException;

public class FirebirdConnection extends BaseConnection<FirebirdDatabase> {

    private static final String DUMMY_SCHEMA_NAME = "default";

    FirebirdConnection(FirebirdDatabase database, java.sql.Connection connection) {
        super(database, connection);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return DUMMY_SCHEMA_NAME;
    }

    @Override
    public Schema<?, ?> getSchema(String name) {
        // database == schema, always return the same dummy schema
        return new FirebirdSchema(jdbcTemplate, database, DUMMY_SCHEMA_NAME);
    }
}
