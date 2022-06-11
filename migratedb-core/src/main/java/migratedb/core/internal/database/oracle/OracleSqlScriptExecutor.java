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
package migratedb.core.internal.database.oracle;

import migratedb.core.api.internal.callback.CallbackExecutor;
import migratedb.core.api.internal.jdbc.JdbcTemplate;
import migratedb.core.internal.sqlscript.DefaultSqlScriptExecutor;

public class OracleSqlScriptExecutor extends DefaultSqlScriptExecutor {

    public OracleSqlScriptExecutor(JdbcTemplate jdbcTemplate, CallbackExecutor callbackExecutor,
                                   boolean outputQueryResults) {
        super(jdbcTemplate, callbackExecutor, outputQueryResults);
    }
}
