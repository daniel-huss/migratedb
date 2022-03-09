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

class GivenStepImpl(private val databaseHandle: DbSystem.Handle) : AutoCloseable, Dsl.GivenStep {
    private var database: DatabaseImpl? = null

    override fun database(block: (DatabaseSpec).() -> Unit) {
        check(database == null) { "Only one database spec, please" }
        database = DatabaseImpl(databaseHandle).also(block)
    }

    override fun close() {
        database.use {}
    }

    fun executeActions() = database!!.materialize().let {
        GivenInfo(
            databaseHandle = databaseHandle,
            database = it.database,
            schemaName = it.schemaName,
            namespace = it.namespace
        )
    }
}
