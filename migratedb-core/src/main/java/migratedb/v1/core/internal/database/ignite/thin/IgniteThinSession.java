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
package migratedb.v1.core.internal.database.ignite.thin;

import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.internal.database.base.BaseSession;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Apache Ignite Thin connection.
 */
public class IgniteThinSession extends BaseSession {
    IgniteThinSession(IgniteThinDatabase database, Connection connection) {
        super(database, connection);
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        getJdbcConnection().setSchema(schema);
    }

    @Override
    public Schema getSchema(String name) {
        return new IgniteThinSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return getJdbcConnection().getSchema();
    }

    @Override
    public IgniteThinDatabase getDatabase() {
        return (IgniteThinDatabase) super.getDatabase();
    }
}
