package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.Db2
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.Derby
import migratedb.integrationtest.database.Hsqldb
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class DerbyParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Derby::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                fromScript(
                    "V1", """
                /* Single line comment */
                CREATE TABLE test_data (
                  value VARCHAR(25) NOT NULL PRIMARY KEY
                );
                
                /*
                Multi-line
                comment
                */
                
                -- Sql-style comment
                
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
