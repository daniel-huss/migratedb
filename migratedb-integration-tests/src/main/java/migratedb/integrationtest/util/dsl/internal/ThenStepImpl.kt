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

package migratedb.integrationtest.util.dsl.internal

import migratedb.core.api.configuration.FluentConfiguration
import migratedb.core.api.internal.schemahistory.AppliedMigration
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.dsl.Dsl
import org.springframework.jdbc.core.JdbcTemplate

class ThenStepImpl<G : Any>(given: G, givenInfo: GivenInfo) : Dsl.ThenStep<G>, AbstractAfterGiven<G>(given, givenInfo) {
    override fun withConnection(block: (JdbcTemplate) -> Unit) {
        givenInfo.database.supportsChangingCurrentSchema()
        givenInfo.databaseHandle
            .newAdminConnection(givenInfo.namespace)
            .work(schema = givenInfo.schemaName, action = block)
    }

    override fun schemaHistory(table: String?, block: (List<AppliedMigration>) -> Unit) {
        val configuration = FluentConfiguration().apply {
            table?.let(::table)
            givenInfo.schemaName?.let { schemas(it.toString()) }
        }
        val schemaHistory = DatabaseImpl.getSchemaHistory(configuration, givenInfo.database)
        block(schemaHistory.allAppliedMigrations())
    }
}
