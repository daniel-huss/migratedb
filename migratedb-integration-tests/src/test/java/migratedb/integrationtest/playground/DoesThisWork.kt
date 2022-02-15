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

import migratedb.core.api.MigrationType.JDBC
import migratedb.integrationtest.IntegrationTest
import migratedb.integrationtest.database.Postgres
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class DoesThisWork : IntegrationTest() {

    @ParameterizedTest
    @EnumSource(Postgres::class)
    fun test(postgres: Postgres) = withDsl {
        given {
            database(postgres) {
                name("test")
                existingSchemaHistory("migratedb") {
                    entry(name = "V001__Test", type = JDBC, true)
                }
            }
        }.`when` {
            migrate {

            }
        }.then {

        }
    }
}
