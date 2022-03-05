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

package migratedb.integrationtest.dsl.internal

import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.dsl.DatabaseSpec
import migratedb.integrationtest.dsl.Dsl
import migratedb.integrationtest.util.container.SharedResources

class GivenStepImpl(private val sharedResources: SharedResources) : AutoCloseable, Dsl.GivenStep {
    private var database: DatabaseImpl? = null
    private var databaseHandle: DbSystem.Handle? = null

    override fun database(dbSystem: DbSystem, block: (DatabaseSpec).() -> Unit) {
        check(databaseHandle == null) { "Only one database, please" }
        databaseHandle = dbSystem.get(sharedResources).also {
            database = DatabaseImpl(it).also(block)
        }
    }

    override fun close() {
        databaseHandle.use {
            database.use {}
        }
    }

    fun executeActions() = database!!.materialize().let {
        GivenInfo(
            databaseHandle = databaseHandle!!,
            database = it.database,
            databaseName = it.databaseName,
            schemaName = it.schemaName
        )
    }
}
