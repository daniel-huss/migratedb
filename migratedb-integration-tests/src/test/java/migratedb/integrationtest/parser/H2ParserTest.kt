package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.H2
import migratedb.integrationtest.database.Oracle
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class H2ParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(H2::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                fromScript(
                    "V1", """
                /* Single line comment */
                CREATE TABLE test_data (
                    name VARCHAR(50) NOT NULL PRIMARY KEY
                );
                
                /*
                Multi-line
                comment
                */
                
                -- Sql-style comment
                
                -- Placeholder
                INSERT INTO ${'$'}{tableName} (name) VALUES ('Mr. T');
                INSERT INTO test_data (name) VALUES ( ${'$'}${'$'}'Mr. Semicolon+Linebreak;
                another line'${'$'}${'$'});
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
