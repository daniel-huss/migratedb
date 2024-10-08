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
import migratedb.v1.integrationtest.database.MariaDb
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class MariaDbParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(MariaDb::class)
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
                     value VARCHAR(25) NOT NULL,
                     PRIMARY KEY(value)
                    );
                    
                    /*
                    Multi-line
                    comment
                    */
                    
                    -- MySQL procedure
                    DELIMITER //
                    CREATE PROCEDURE AddData()
                     BEGIN
                       # MySQL-style single line comment
                       INSERT INTO test_data (value) VALUES ('Hello');
                     END //
                    DELIMITER;
                    
                    CALL AddData();
                    
                    -- MySQL comments directives generated by mysqlsump
                    /*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
                    /*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
                    
                    -- Placeholder
                    INSERT INTO ${'$'}{tableName} (value) VALUES ('Mr. T');
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

    @ParameterizedTest
    @EnumSource(MariaDb::class)
    fun `Process engine example can be parsed`(dbSystem: DbSystem) {
        ProcessEngineTestCase(dbSystem)
    }
}
