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
import migratedb.commandline.testing.TestMigrationsJar
import migratedb.commandline.testing.migration.V1__First_Migration
import migratedb.commandline.testing.migration.V2__Second_Migration
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
        defaultMigrationsDir.resolve("V1__First.sql").writeText("create table _script1(id int)")
        defaultMigrationsDir.resolve("V2__Second.sql").writeText("create table _script2(id int)")
        defaultMigrationsDir.resolve("subdir").resolve("V3__Third.sql").apply {
            parentFile.mkdirs()
            writeText("create table _script3(id int)")
        }
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        exec("-url=$dbUrl", "-user=sa", "-password=", "-validateMigrationNaming=true", "migrate").asClue {
            it.exitCode.shouldBe(0)
            withDatabase(dbUrl) { db ->
                db.tablesInCurrentSchema()
                    .map(String::lowercase)
                    .shouldContainAll("_script1", "_script2", "_script3")
            }
        }
    }

    @Test
    fun `Default jars directory is being scanned for migration code`() = withCommandLine {
        defaultJarsDir.shouldBeADirectory()
        TestMigrationsJar.copyTo(defaultJarsDir.resolve("my-migrations.jar"))
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        exec("-url=$dbUrl", "-user=sa", "-password=", "-locations=db/migration", "migrate").asClue {
            it.exitCode.shouldBe(0)
            withDatabase(dbUrl) { db ->
                db.tablesInCurrentSchema()
                    .map(String::lowercase)
                    .shouldContainAll(
                        V1__First_Migration.CREATED_TABLE.lowercase(),
                        V2__Second_Migration.CREATED_TABLE.lowercase()
                    )
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
