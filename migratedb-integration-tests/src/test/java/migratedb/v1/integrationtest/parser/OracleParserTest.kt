package migratedb.v1.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.database.Oracle
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class OracleParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Oracle::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                usingScript(
                    "V1",
                    """
                    /* Single line comment */
                    CREATE TABLE test_user (
                      name VARCHAR(25) NOT NULL,
                      PRIMARY KEY(name)
                    );
                    
                    /*
                    Multi-line
                    comment
                    */
                    -- PL/SQL block
                    CREATE TRIGGER test_trig AFTER insert ON test_user
                    BEGIN
                       UPDATE test_user SET name = CONCAT(name, ' triggered');
                    END;
                    /
                    
                    -- Placeholder
                    INSERT INTO ${'$'}{tableName} (name) VALUES ('Mr. T');
                """.trimIndent()
                )
                withConfig {
                    placeholders(mapOf("tableName" to "test_user"))
                }
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
        }
    }

    @ParameterizedTest
    @EnumSource(Oracle::class)
    fun `Process engine example can be parsed`(dbSystem: DbSystem) {
        ProcessEngineTestCase(dbSystem)
    }
}
