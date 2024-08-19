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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.dsl.DatabaseSpec
import migratedb.v1.integrationtest.util.dsl.Dsl
import java.sql.Connection

class GivenStepImpl(private val databaseInstance: () -> DbSystem.Instance) : AutoCloseable, Dsl.GivenStep {
    private var database: DatabaseImpl? = null
    private var databaseContext: DatabaseContext? = null
    private val extensions = mutableListOf<(DatabaseContext) -> Unit>()

    override fun database(block: (DatabaseSpec).() -> Unit) {
        check(database == null) { "Only one database spec, please" }
        database = DatabaseImpl(databaseInstance()).also(block)
    }

    override fun independentDbMutation(): IndependentDatabaseMutation {
        val delegate = lazy {
            databaseInstance().nextMutation(databaseContext!!.schemaName)
        }
        return object : IndependentDatabaseMutation {
            override fun isApplied(connection: Connection) = delegate.value.isApplied(connection)

            override fun apply(connection: Connection) = delegate.value.apply(connection)

            override fun undo(connection: Connection) = delegate.value.undo(connection)
        }
    }

    override fun extend(extension: (DatabaseContext) -> Unit) {
        extensions.add(extension)
    }

    override fun close() {
        database.use {}
    }

    fun executeActions() = requireNotNull(database) {
        "Forgot to call database { } within given { }!"
    }.materialize().let { materializeResult ->
        DatabaseContext(
            databaseInstance = databaseInstance(),
            database = materializeResult.database,
            adminDataSource = materializeResult.adminDataSource,
            schemaName = materializeResult.schemaName,
            namespace = materializeResult.namespace
        ).also {
            databaseContext = it
            materializeResult.initializer?.let { initializer ->
                it.adminDataSource.connection.use(initializer)
            }
            extensions.forEach { extension ->
                extension(it)
            }
            extensions.clear()
        }
    }
}
