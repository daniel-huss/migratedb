/*
 * Copyright 2022 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.integrationtest.api

import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import migratedb.core.api.migration.ScriptMigration
import migratedb.integrationtest.database.SomeInMemoryDb
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.api.Test
import java.io.Reader

class ScriptMigrationTest : IntegrationTest() {
    @Test
    fun `getChecksum() replaces placeholders`() = withDsl(SomeInMemoryDb) {
        given {
            database {}
        }.`when` {
            val migration = V001__Test()
            fun checksum(value: String) = justRun {
                withConfig {
                    placeholderPrefix("%{")
                    placeholderSuffix("}")
                    placeholders(mapOf("placeholder" to value))
                    migration.getChecksum(this)
                }
            }
            checksum("a") to checksum("b")
        }.then { (first, second) ->
            first.shouldNotBe(second)
        }
    }

    @Test
    fun `migrate() replaces placeholders`() = withDsl(SomeInMemoryDb) {
        given {
            database {}
        }.`when` {
            migrate {
                withConfig {
                    placeholderPrefix("%{")
                    placeholderSuffix("}")
                    placeholders(mapOf("placeholder" to "value"))
                }
                fromCode("V001_Test", V001__Test())
            }
        }.then {
            it.migrationsExecuted.shouldBe(1)
            withConnection { jdbc ->
                jdbc.queryForObject("select t from t", String::class.java)
                    .shouldBe("value")
            }
        }
    }

    @Test
    fun `script() can be a Reader which is properly closed`() = withDsl(SomeInMemoryDb) {
        val migration = V002__Test()
        given {
            database {}
        }.`when` {
            migrate {
                fromCode("V002_Test", migration)
            }
        }.then {
            migration.closed.apply {
                shouldNotBeEmpty()
                shouldForAll { closed -> closed.shouldBeTrue() }
            }
        }
    }


    class V001__Test : ScriptMigration() {
        override fun script() = object : Any() {
            override fun toString() = """
            create table t (t varchar primary key);
            insert into t(t) values ('%{placeholder}');
        """.trimIndent()
        }
    }

    class V002__Test : ScriptMigration() {
        var closed = mutableListOf<Boolean>()

        override fun script(): Any {
            val index = closed.size
            closed.add(false)
            return object : Reader() {
                override fun close() {
                    closed[index] = true
                }

                override fun read(cbuf: CharArray, off: Int, len: Int) = -1
            }
        }
    }
}
