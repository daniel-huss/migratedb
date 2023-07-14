package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.Sqlite
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SqliteParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Sqlite::class)
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
                
                -- Sql-style comment
                
                -- Placeholder
                INSERT INTO ${'$'}{tableName} (value) VALUES ('Mr. T');
                
                CREATE TABLE customers(name VARCHAR, address VARCHAR);
                CREATE TABLE orders(address VARCHAR, customer_name VARCHAR);
                CREATE TRIGGER update_customer_address UPDATE OF address ON customers
                  BEGIN
                    UPDATE orders SET address = new.address WHERE customer_name = old.name;
                  END;
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
