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

package migratedb.integrationtest.playground

import io.kotest.matchers.shouldBe
import migratedb.core.api.MigrationType.JDBC
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.util.base.IntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SelfTest : IntegrationTest() {
    @ParameterizedTest
    @ArgumentsSource(DbSystem.All::class)
    fun databaseIsSupported(dbSystem: DbSystem) = withDsl {
        given {
            database(dbSystem) {
                existingSchemaHistory("migratedb") {
                    entry(name = "V001__Test", type = JDBC, success = true)
                }
            }
        }.`when` {
            migrate {
                withConfig {
                    it.table("migratedb")
                    it.ignoreMissingMigrations(true)
                }
                script("V002__Foo", "-- This script does nothing")
            }
        }.then {
            withConnection {
                it.queryForList("select * from migratedb").size.shouldBe(2)
            }
        }
    }
}
