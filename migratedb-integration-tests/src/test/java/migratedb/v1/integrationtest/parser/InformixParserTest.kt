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
import migratedb.v1.integrationtest.database.*
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class InformixParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Informix::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                usingScript(
                    "V1",
                    """
                /* Single line comment */
                CREATE SEQUENCE seq_2
                   INCREMENT BY 1 START WITH 1
                   MAXVALUE 30 MINVALUE 0
                   NOCYCLE CACHE 10 ORDER;
                
                CREATE TABLE tab1 (col1 int, col2 int);
                INSERT INTO tab1 VALUES (0, 0);
                
                INSERT INTO tab1 (col1, col2) VALUES (seq_2.NEXTVAL, seq_2.NEXTVAL);
                
                /*
                Multi-line
                comment
                */
                -- SPL
                CREATE PROCEDURE raise_prices ( per_cent INT, selected_unit CHAR )
                    UPDATE stock SET unit_price = unit_price + (unit_price * (per_cent/100) )
                    where unit=selected_unit;
                END PROCEDURE;
                
                CREATE FUNCTION square ( num INT )
                   RETURNING INT;
                   return (num * num);
                END FUNCTION
                   DOCUMENT "USAGE: Update a price by a percentage",
                         "Enter an integer percentage from 1 - 100",
                         "and a part id number";
                
                -- Placeholder
                INSERT INTO ${'$'}{tableName} (col1, col2) VALUES (2, 3);
                """.trimIndent()
                )
                withConfig {
                    placeholders(mapOf("tableName" to "tab1"))
                }
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
        }
    }
}
