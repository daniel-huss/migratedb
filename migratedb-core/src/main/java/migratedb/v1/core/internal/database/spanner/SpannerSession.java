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
package migratedb.v1.core.internal.database.spanner;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.internal.database.base.BaseSession;

import java.sql.Connection;
import java.util.concurrent.Callable;

public class SpannerSession extends BaseSession {
    protected SpannerSession(SpannerDatabase database, Connection connection) {
        super(database, connection);
        this.jdbcTemplate = new SpannerJdbcTemplate(connection, database.getDatabaseType());
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() {
        return "";
    }

    @Override
    public SpannerSchema getSchema(String name) {
        return new SpannerSchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public SpannerDatabase getDatabase() {
        return (SpannerDatabase) super.getDatabase();
    }

    @Override
    public <T> T lock(Table table, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            RuntimeException rethrow;
            if (e instanceof RuntimeException) {
                rethrow = (RuntimeException) e;
            } else {
                rethrow = new MigrateDbException(e);
            }
            throw rethrow;
        }
    }
}
