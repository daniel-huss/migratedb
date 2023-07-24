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
package migratedb.v1.core.internal.database.base;

import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Function;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.util.StringUtils;

public abstract class BaseFunction<D extends Database<?>, S extends Schema<?, ?>> extends BaseSchemaObject<D, S>
        implements Function<D, S> {
    protected final String[] args;

    public BaseFunction(JdbcTemplate jdbcTemplate, D database, S schema, String name, String... args) {
        super(jdbcTemplate, database, schema, name);
        this.args = args == null ? new String[0] : args;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + StringUtils.arrayToCommaDelimitedString(args) + ")";
    }
}
