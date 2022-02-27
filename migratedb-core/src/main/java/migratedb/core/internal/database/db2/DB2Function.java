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
package migratedb.core.internal.database.db2;

import java.sql.SQLException;
import migratedb.core.api.internal.database.base.Database;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.internal.database.base.BaseFunction;

/**
 * DB2-specific function.
 */
public class DB2Function extends BaseFunction {
    /**
     * Creates a new Db2 function.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this function lives in.
     * @param name         The name of the function.
     * @param args         The arguments of the function.
     */
    DB2Function(JdbcTemplate jdbcTemplate, Database database, Schema schema, String name, String... args) {
        super(jdbcTemplate, database, schema, name, args);
    }

    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP SPECIFIC FUNCTION " + database.quote(schema.getName(), name));
    }
}
