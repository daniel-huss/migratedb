package migratedb.v1.spring.boot.v3.autoconfig

import com.fasterxml.jackson.databind.json.JsonMapper
import migratedb.v1.core.MigrateDb
import migratedb.v1.core.api.migration.ScriptMigration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}

/**
 * A spring boot application for manual testing.
 */
@SpringBootApplication
@PropertySource("classpath:test-application.yaml")
class TestApplication {
    @Bean
    fun jsonMapper(): JsonMapper = JsonMapper.builder().build()

    @RestController
    @Transactional
    class Endpoint(val dsl: DSLContext, val migrateDb: MigrateDb, val jsonMapper: JsonMapper) {
        @PostMapping("info", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun info(): String {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(migrateDb.info())
        }

        @PostMapping("migrate", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun migrate(): String {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(migrateDb.migrate())
        }

        @PostMapping("selectFoo", produces = [MediaType.APPLICATION_JSON_VALUE])
        fun selectFoo(): List<Map<String, *>> {
            return dsl.select(DSL.asterisk())
                .from(DSL.table("foo"))
                .fetchMaps()
        }
    }

    @Component
    class InfoInsteadOfMigrate(val jsonMapper: JsonMapper) : MigrateDbExecution {
        override fun run(migrateDb: MigrateDb) {
            println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(migrateDb.info().infoResult))
        }
    }

    @Component
    class V000__Initial_Schema : ScriptMigration() {
        override fun script() = """
            create table foo(id varchar primary key)        
                """.trimIndent()
    }

    @Component
    class V001__First_Change : ScriptMigration() {
        override fun script() = """
            alter table foo add column another_col varchar default 'hello'        
                """.trimIndent()
    }

    @Component
    class V002__Second_Change : ScriptMigration() {
        override fun script() = """
            insert into foo(id, another_col) values ('1', 'first'), ('2', 'second') 
                """.trimIndent()
    }

    @Component
    class B001__Baseline : ScriptMigration() {
        override fun script() = """
           create table foo(id varchar primary key, another_col varchar default 'hello') 
        """.trimIndent()
    }
}
