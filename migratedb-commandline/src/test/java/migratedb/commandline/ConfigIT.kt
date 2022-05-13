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

package migratedb.commandline

import io.kotest.assertions.asClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import migratedb.commandline.testing.CommandLineTest
import migratedb.commandline.testing.Dsl
import migratedb.core.api.configuration.PropertyNames
import migratedb.core.api.output.BaselineResult
import migratedb.core.internal.configuration.ConfigUtils
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.util.*

class ConfigIT : CommandLineTest() {
    @Test
    fun `Default config file exists`() = withCommandLine {
        defaultConfigFile.shouldBeAFile()
    }

    @Test
    fun `Default config file is being used `() = withCommandLine {
        changeDefaultConfig {
            it[PropertyNames.URL] = "jdbc:h2:mem:blargh"
            it[PropertyNames.BASELINE_VERSION] = "17"
        }
        exec("-outputType=json", "baseline").asClue {
            it.exitCode.shouldBe(0)
            it.parseAs(BaselineResult::class).apply {
                baselineVersion.shouldBe("17")
                successfullyBaselined.shouldBeTrue()
                database.shouldBeEqualIgnoringCase("blargh")
            }
        }
    }

    @Test
    fun `Default migrations directory is being scanned for sql files`() = withCommandLine {
        defaultMigrationsDir.shouldBeADirectory()
        defaultMigrationsDir.resolve("V1__First.sql")
            .writeText("create table _first(id int)")
        defaultMigrationsDir.resolve("V2__Second.sql")
            .writeText("create table _second(id int)")
        defaultMigrationsDir.resolve("subdir").resolve("V3__Third.sql").apply {
            parentFile.mkdirs()
            writeText("create table _third(id int)")
        }
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        exec("-url=$dbUrl", "-user=sa", "-password=", "-validateMigrationNaming=true", "migrate").asClue {
            it.exitCode.shouldBe(0)
            withDatabase(dbUrl) { sql ->
                sql.query(
                    """select table_name 
                    | from information_schema.tables s
                    | where s.table_schema = CURRENT_SCHEMA""".trimMargin()
                ) { rs, _ -> rs.getString(1) }
                    .map(String::lowercase)
                    .shouldContainAll("_first", "_second", "_third")
            }
        }
    }

    private val Dsl.defaultConfigFile get() = configDir.resolve("migratedb.conf")
    private fun Dsl.changeDefaultConfig(block: (MutableMap<String, String>) -> Unit) {
        val config = defaultConfigFile.reader().use {
            LinkedHashMap(ConfigUtils.loadConfiguration(it))
        }
        block(config)
        Properties().apply {
            putAll(config)
            val text = StringWriter().use {
                store(it, null)
                it.toString().replace("\\:", ":")
            }
            defaultConfigFile.writeText(text)
        }
    }
}
