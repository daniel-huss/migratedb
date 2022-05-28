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
import io.kotest.assertions.print.print
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import migratedb.commandline.testing.CommandLineTest
import migratedb.core.api.MigrationState
import migratedb.core.api.output.InfoResult
import org.junit.jupiter.api.Test

class InfoIT : CommandLineTest() {
    @Test
    fun `Can run info command`() = withCommandLine {
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        defaultMigrationsDir.resolve("V1__V1.sql").writeText("")
        exec("-outputType=json", "-url=$dbUrl", "-user=sa", "-password=", "info").asClue { output ->
            output.exitCode.shouldBe(0)
            val actual = output.parseAs(InfoResult::class)
            withClue(actual.print().value) {
                actual.allSchemasEmpty.shouldBeTrue()
                actual.migrations.shouldBeSingleton {
                    it.version.shouldBe("1")
                    it.state.shouldBe(MigrationState.PENDING.displayName)
                }
            }
        }
    }
}
