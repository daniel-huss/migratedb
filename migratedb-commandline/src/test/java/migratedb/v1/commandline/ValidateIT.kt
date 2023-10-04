/*
 * Copyright 2022-2023 The MigrateDB contributors
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
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import migratedb.v1.commandline.testing.CommandLineTest
import migratedb.v1.core.api.ErrorCode
import migratedb.v1.core.api.output.ValidateResult
import org.junit.jupiter.api.Test

class ValidateIT : CommandLineTest() {
    @Test
    fun `Can run validate command`() = withCommandLine {
        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        defaultMigrationsDir.resolve("V1__V1.sql").writeText("")
        exec("-outputType=json", "-url=$dbUrl", "-user=sa", "-password=", "validate").asClue { output ->
            output.exitCode.shouldBe(0)
            val actual = output.parseAs(ValidateResult::class)
            withClue(actual.print().value) {
                actual.validationSuccessful.shouldBeFalse()
                actual.invalidMigrations.shouldBeSingleton {
                    it.version.shouldBe("1")
                    it.errorDetails.shouldNotBeNull()
                        .errorCode.shouldBe(ErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED)
                }
            }
        }
    }
}
