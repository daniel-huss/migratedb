package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.Firebird
import migratedb.integrationtest.util.base.IntegrationTest
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
