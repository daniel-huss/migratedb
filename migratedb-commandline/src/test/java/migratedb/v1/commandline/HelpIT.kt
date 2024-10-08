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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import migratedb.v1.commandline.testing.CommandLineTest
import migratedb.v1.testing.util.base.Args
import migratedb.v1.testing.util.base.Args.OneParam.YES
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class HelpIT : CommandLineTest() {
    @ParameterizedTest
    @ArgumentsSource(HelpArgs::class)
    fun `Prints usage`(args: List<String>) = withCommandLine {
        val output = exec(*args.toTypedArray())
        output.asClue {
            it.exitCode.shouldBe(0)
            it.stdOut.first().shouldStartWith("Usage")
        }
    }

    class HelpArgs : Args(
        functionHasOneParameter = YES,
        emptyList<String>(),
        listOf("help"),
        listOf("--help")
    )
}
