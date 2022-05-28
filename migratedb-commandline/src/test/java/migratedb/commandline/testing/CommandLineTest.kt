/*
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
package migratedb.commandline.testing

import migratedb.testing.util.base.AbstractTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.DriverManager

abstract class CommandLineTest : AbstractTest() {
    fun withCommandLine(block: (Dsl).() -> Unit) {
        Dsl().use {
            block(it)
        }
    }

    fun <T> withDatabase(url: String, user: String = "sa", password: String = "", block: (JdbcTemplate) -> T) {
        DriverManager.getConnection(url, user, password).use {
            block(JdbcTemplate(SingleConnectionDataSource(it, true)))
        }
    }

    fun JdbcTemplate.tablesInCurrentSchema(): List<String> {
        return query("select table_name from information_schema.tables s where s.table_schema = CURRENT_SCHEMA") { rs, _ ->
            rs.getString(1)
        }
    }
}
