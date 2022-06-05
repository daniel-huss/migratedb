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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import migratedb.commandline.testing.CommandLineTest
import migratedb.core.api.output.LiberateResult
import org.junit.jupiter.api.Test

class LiberateIT : CommandLineTest() {
    @Test
    fun `Can run liberate command`() = withCommandLine {
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
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
                actual.actions?.shouldBeEmpty()
            }
        }
    }
}
