/*
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

package migratedb.v1.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.database.Firebird
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class FirebirdParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Firebird::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                usingScript(
                    "V1",
                    """
                /* Single line comment */
                CREATE TABLE test_data (
                  name VARCHAR(25) NOT NULL PRIMARY KEY
                );
                 /*
                Multi-line
                comment
                */
                 -- Sql-style comment
                 -- Placeholder
                ALTER TABLE ${'$'}{tableName} ADD id INT NOT NULL;
                 -- Terminator changes
                SET TERM #;
                CREATE OR ALTER PROCEDURE SHIP_ORDER (
                    PO_NUM CHAR(8))
                AS
                BEGIN
                  /* Stored procedure body */
                END#
                SET TERM ;#
                """.trimIndent()
                )
                withConfig {
                    placeholders(mapOf("tableName" to "test_data"))
                }
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
        }
    }
}
