package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.Db2
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.Hsqldb
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class Db2ParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Db2::class)
    fun `Example from docs can be parsed`(dbSystem: DbSystem) = withDsl(dbSystem) {
        given {
            database { }
        }.`when` {
            migrate {
                fromScript(
                    "V1", """
                    /* Single line comment */
                    CREATE TABLE usertable (
                     name VARCHAR(25) NOT NULL,
                     PRIMARY KEY(name)
                    );
                    
                    /*
                    Multi-line
                    comment
                    */
                    
                    -- Placeholder
                    INSERT INTO ${'$'}{tableName} (name) VALUES ('Mr. T');
                    
                    -- SQL-PL
                    CREATE TRIGGER uniqueidx_trigger BEFORE INSERT ON usertable
                        REFERENCING NEW ROW AS newrow
                        FOR EACH ROW WHEN (newrow.name is not null)
                        BEGIN ATOMIC
                          IF EXISTS (SELECT * FROM usertable WHERE usertable.name = newrow.name) THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'duplicate name';
                          END IF;
                        END;
                    
                    -- Terminator changes
                    --#SET TERMINATOR @
                    CREATE FUNCTION TEST_FUNC(PARAM1 INTEGER, PARAM2 INTEGER)
                      RETURNS INTEGER
                    LANGUAGE SQL
                      RETURN
                      1@   
                    --#SET TERMINATOR ;
                    CREATE FUNCTION TEST_FUNC(PARAM1 INTEGER, PARAM2 INTEGER, PARAM3 INTEGER)
                      RETURNS INTEGER
                    LANGUAGE SQL
                      RETURN
                      1;
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
