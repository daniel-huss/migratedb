package migratedb.v1.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.v1.integrationtest.database.*
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SqlServerParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(SqlServer::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                usingScript(
                    "V1",
                    """
                    /* Single line comment */
                    CREATE TABLE Customers (
                    CustomerId smallint identity(1,1),
                    Name nvarchar(255),
                    Priority tinyint
                    )
                    CREATE TABLE Sales (
                    TransactionId smallint identity(1,1),
                    CustomerId smallint,
                    [Net Amount] int,
                    Completed bit
                    )
                    GO
                    
                    /*
                    Multi-line
                    comment
                    */
                    -- TSQL
                    CREATE TRIGGER dbo.Update_Customer_Priority
                     ON dbo.Sales
                    AFTER INSERT, UPDATE, DELETE
                    AS
                    WITH CTE AS (
                     select CustomerId from inserted
                     union
                     select CustomerId from deleted
                    )
                    UPDATE Customers
                    SET
                     Priority =
                       case
                         when t.Total < 10000 then 3
                         when t.Total between 10000 and 50000 then 2
                         when t.Total > 50000 then 1
                         when t.Total IS NULL then NULL
                       end
                    FROM Customers c
                    INNER JOIN CTE ON CTE.CustomerId = c.CustomerId
                    LEFT JOIN (
                     select
                       Sales.CustomerId,
                       SUM([Net Amount]) Total
                     from Sales
                     inner join CTE on CTE.CustomerId = Sales.CustomerId
                     where
                       Completed = 1
                     group by Sales.CustomerId
                    ) t ON t.CustomerId = c.CustomerId
                    GO
                    
                    -- Placeholder
                    INSERT INTO ${'$'}{tableName} (Name, Priority) VALUES ('Mr. T', 1);
                """.trimIndent()
                )
                withConfig {
                    placeholders(mapOf("tableName" to "Customers"))
                }
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
        }
    }

    @ParameterizedTest
    @EnumSource(SqlServer::class)
    fun `Process engine example can be parsed`(dbSystem: DbSystem) {
        ProcessEngineTestCase(dbSystem)
    }
}
