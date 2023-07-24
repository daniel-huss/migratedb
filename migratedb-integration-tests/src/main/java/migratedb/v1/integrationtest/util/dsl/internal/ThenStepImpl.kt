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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.integrationtest.util.base.work
import migratedb.v1.integrationtest.util.dsl.Dsl
import migratedb.v1.core.api.configuration.FluentConfiguration
import migratedb.v1.core.api.internal.schemahistory.AppliedMigration
import migratedb.v1.core.internal.jdbc.JdbcConnectionFactoryImpl
import org.springframework.jdbc.core.JdbcTemplate

class ThenStepImpl<G : Any>(given: G, databaseContext: DatabaseContext) : Dsl.ThenStep<G>,
    AbstractAfterGiven<G>(given, databaseContext) {
    override fun withConnection(block: (JdbcTemplate) -> Unit) {
        databaseContext.database.supportsChangingCurrentSchema()
        databaseContext.databaseHandle
            .newAdminConnection(databaseContext.namespace)
            .work(schema = databaseContext.schemaName, action = block)
    }

    override fun schemaHistory(table: String?, block: (List<AppliedMigration>) -> Unit) {
        val configuration = FluentConfiguration().apply {
            table?.let(::table)
            databaseContext.schemaName?.let { schemas(it.toString()) }
        }
        // JdbcConnectionFactoryImpl always opens a connection, creating a leak if not closed...
        val connectionFactory = JdbcConnectionFactoryImpl(databaseContext.adminDataSource::getConnection, configuration)
        connectionFactory.openConnection().use { }

        // Do not re-use database from givenInfo because its connection might not observe the effects of previously
        // committed transactions.
        databaseContext.database.databaseType.createDatabase(
            configuration,
            connectionFactory
        ).use {
            val schemaHistory = DatabaseImpl.getSchemaHistory(configuration, databaseContext.database)
            block(schemaHistory.allAppliedMigrations())
        }
    }
}
