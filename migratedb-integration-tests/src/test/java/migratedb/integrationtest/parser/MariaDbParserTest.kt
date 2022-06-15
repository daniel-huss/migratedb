package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.MariaDb
import migratedb.integrationtest.util.base.IntegrationTest
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
                fromScript(
                    "V1", """
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
}
