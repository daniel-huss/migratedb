package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.Postgres
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PostgresParserTest : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Postgres::class)
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
                
                -- Multi-statement PostgreSQL function
                CREATE FUNCTION AddData() RETURNS INTEGER
                AS ${'$'}${'$'}
                   BEGIN
                    INSERT INTO test_data (value) VALUES ('Hello');
                    RETURN 1;
                  END;
                ${'$'}${'$'} LANGUAGE plpgsql;
                
                SELECT *  INTO TEMP adddata_temp_table FROM AddData() ;
                
                -- Single-statement PostgreSQL function
                CREATE FUNCTION add(integer, integer) RETURNS integer
                   LANGUAGE sql IMMUTABLE STRICT
                   AS ${'$'}_${'$'}select ${'$'}1 + ${'$'}2;${'$'}_${'$'};
                
                -- Placeholder
                INSERT INTO ${'$'}{tableName} (value) VALUES ('Mr. T');
                
                -- COPY ... FROM STDIN
                CREATE TABLE copy_test(c1 text, c2 text, c3 text);
                COPY copy_test (c1, c2, c3) FROM stdin;
                1	utf8: ümlaute: äüß	NaN
                2	\N	123
                3	text	123.234444444444449
                \.
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
    @EnumSource(Postgres::class)
    fun `Process engine example can be parsed`(dbSystem: DbSystem) {
        ProcessEngineTestCase(dbSystem)
    }
}
