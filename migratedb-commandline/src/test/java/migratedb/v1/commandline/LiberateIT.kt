/*
 * Copyright 2022-2024 The MigrateDB contributors
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

package migratedb.v1.commandline

import io.kotest.assertions.asClue
import io.kotest.assertions.print.print
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import migratedb.v1.commandline.testing.CommandLineTest
import migratedb.v1.core.api.output.LiberateResult
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.logging.multi.MultiLogger
import org.junit.jupiter.api.Test

class LiberateIT : CommandLineTest() {
    @Test
    fun `Can run liberate command`() = withCommandLine {
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        withDatabase(dbUrl) {
            LogFactory.setLogCreator { MultiLogger(emptyList()) } // No logging, please
            Flyway.configure()
                .dataSource(it.dataSource)
                .table("old_table")
                .baselineVersion("14")
                .load()
                .baseline()
        }
        exec(
            "-outputType=json",
            "-url=$dbUrl",
            "-user=sa",
            "-password=",
            "-oldTable=old_table",
            "-table=new_table",
            "liberate"
        ).asClue { output ->
            output.exitCode.shouldBe(0)
            val actual = output.parseAs(LiberateResult::class)
            withClue(actual.print().value) {
                actual.oldSchemaHistoryTable.shouldBe("old_table")
                actual.schemaHistoryTable.shouldBe("new_table")
            }
        }
    }
}
