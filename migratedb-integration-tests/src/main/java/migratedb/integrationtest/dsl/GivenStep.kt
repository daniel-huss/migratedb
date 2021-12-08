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

package migratedb.integrationtest.dsl

import migratedb.integrationtest.SharedResources
import migratedb.integrationtest.database.SupportedDatabase

class GivenStep(private val sharedResources: SharedResources) : DslCallback {
    private var database: DatabaseSpec? = null

    fun database(db: SupportedDatabase, block: (DatabaseSpec).() -> Unit) {
        database = DatabaseSpec(sharedResources, db).also(block)
    }

    override fun beforeWhen() = sequenceOf(database).forEach { it?.beforeWhen() }
    override fun beforeThen() = sequenceOf(database).forEach { it?.beforeThen() }
    override fun cleanup() = sequenceOf(database).forEach { it?.cleanup() }
}
