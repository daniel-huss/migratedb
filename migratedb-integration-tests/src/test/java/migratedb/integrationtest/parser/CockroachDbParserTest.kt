package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.CockroachDb
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class CockroachDbParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(CockroachDb::class)
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
                 value VARCHAR(25) NOT NULL PRIMARY KEY
                );
                
                
                /*
                Multi-line
                comment
                */
                
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
    @EnumSource(CockroachDb::class)
    fun `Process engine example can be parsed`(dbSystem: DbSystem) {
        // If we don't skip the drop script, Cockroachdb barfs up errors like
        // "referencing constraint ... in the middle of being added, try again later"
        ProcessEngineTestCase(dbSystem, skipDrop = true)
    }
}
