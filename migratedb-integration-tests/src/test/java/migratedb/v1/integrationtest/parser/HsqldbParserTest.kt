package migratedb.v1.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.database.Hsqldb
import migratedb.v1.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class HsqldbParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Hsqldb::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                usingScript(
                    "V1",
                    """
                /* Single line comment */
                CREATE TABLE usertable (
                  name VARCHAR(25) NOT NULL PRIMARY KEY
                );
                
                /*
                Multi-line
                comment
                */
                
                -- Sql-style comment
                
                -- Placeholder
                INSERT INTO ${'$'}{tableName} (name) VALUES ('Mr. T');
                
                CREATE TRIGGER uniqueidx_trigger BEFORE INSERT ON usertable
                    REFERENCING NEW ROW AS newrow
                    FOR EACH ROW WHEN (newrow.name is not null)
                    BEGIN ATOMIC
                      IF EXISTS (SELECT * FROM usertable WHERE usertable.name = newrow.name) THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'duplicate name';
                      END IF;
                    END;
                """.trimIndent()
                )
                withConfig {
                    placeholders(mapOf("tableName" to "usertable"))
                }
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
        }
    }
}
