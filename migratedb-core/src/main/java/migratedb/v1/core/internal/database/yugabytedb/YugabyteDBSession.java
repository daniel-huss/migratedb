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
package migratedb.v1.core.internal.database.yugabytedb;

import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.internal.database.postgresql.PostgreSQLSession;

import java.sql.Connection;

public class YugabyteDBSession extends PostgreSQLSession {

    YugabyteDBSession(Configuration configuration, YugabyteDBDatabase database, Connection connection) {
        super(configuration, database, connection);
    }

    @Override
    public YugabyteDBSchema getSchema(String name) {
        return new YugabyteDBSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public YugabyteDBDatabase getDatabase() {
        return (YugabyteDBDatabase) super.getDatabase();
    }
}
