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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import migratedb.v1.commandline.testing.CommandLineTest
import migratedb.v1.core.api.ErrorCode
import org.junit.jupiter.api.Test

class DownloadDriversIT : CommandLineTest() {
    @Test
    fun `Downloaded drivers can be used immediately`() = withCommandLine {
        exec(
            "-X",
            "-outputType=json",
            "-driverNames=hsqldb",
            "-url=jdbc:hsqldb:mem:test",
            "-user=sa",
            "-password=secret",
            "download-drivers",
            "migrate"
        ).asClue { output ->
            output.exitCode.shouldBe(0)
            @Suppress("UNCHECKED_CAST")
            (output.parseAs(Map::class)["individualResults"] as List<Map<String, Any?>>).map {
                it["operation"]
            }.shouldContainExactly("download-drivers", "migrate")
        }
    }

    @Test
    fun `Downloads all drivers if none cherry-picked`() = withCommandLine {
        exec("-outputType=json", "download-drivers").asClue {
            it.exitCode.shouldBe(0)
            it.parseAs(DownloadDriversCommand.Result::class)
                .downloadedDrivers
                .shouldContainExactlyInAnyOrder(driversInSpec)
        }
    }

    @Test
    fun `Fails on unsupported driver name`() = withCommandLine {
        exec("-outputType=json", "-driverNames=unsupported", "download-drivers").asClue {
            it.exitCode.shouldNotBe(0)
            it.parseAs(ErrorOutput::class).error.errorCode.shouldBe(ErrorCode.ERROR)
        }
    }

    @Test
    fun `Can cherry-pick drivers with driverNames argument`() = withCommandLine {
        exec("-outputType=json", "-driverNames=mssql,postgresql,hsqldb", "download-drivers").asClue {
            it.exitCode.shouldBe(0)
            it.parseAs(DownloadDriversCommand.Result::class)
                .downloadedDrivers
                .shouldContainExactlyInAnyOrder("mssql", "postgresql", "hsqldb")
        }
    }
}
