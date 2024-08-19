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
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import migratedb.v1.commandline.testing.CommandLineTest
import migratedb.v1.commandline.testing.TestMigrationsJar
import migratedb.v1.commandline.testing.migration.V1__First_Migration
import migratedb.v1.commandline.testing.migration.V2__Second_Migration
import org.junit.jupiter.api.Test

class MigrateIT : CommandLineTest() {
    @Test
    fun `Can run multiple times to reach latest target`() = withCommandLine {
        TestMigrationsJar.copyTo(defaultJarsDir.resolve("v1_and_v2.jar"))
        defaultMigrationsDir.resolve("V3__Third_Migration.sql")
            .writeText("create table _third_from_script(id int)")

        val dbUrl = "jdbc:h2:${installationDir.resolve("mydb.h2").absolutePath}"
        val args = listOf("-X", "-url=$dbUrl", "-user=sa", "-password=", "-locations=filesystem:sql,db/migration")
        exec(args + listOf("-target=1", "migrate")).asClue {
            it.exitCode.shouldBe(0)
        }
        exec(args + listOf("-target=2", "migrate")).asClue {
            it.exitCode.shouldBe(0)
        }
        exec(args + listOf("-target=latest", "migrate")).asClue {
            it.exitCode.shouldBe(0)
        }
        withDatabase(dbUrl) { db ->
            db.tablesInCurrentSchema().map { it.lowercase() }
                .shouldContainAll(
                    V1__First_Migration.CREATED_TABLE.lowercase(),
                    V2__Second_Migration.CREATED_TABLE.lowercase(),
                    "_third_from_script"
                )
        }
    }
}
