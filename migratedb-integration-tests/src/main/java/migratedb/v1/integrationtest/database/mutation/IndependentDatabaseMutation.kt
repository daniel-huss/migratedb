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

package migratedb.v1.integrationtest.database.mutation

import java.sql.Connection

/**
 * An arbitrary database schema mutation that does not interfere with or depend on other such mutations.
 */
interface IndependentDatabaseMutation {
    /**
     * @return `true` iff the mutation has been applied and its effects are visible to [connection].
     */
    fun isApplied(connection: Connection): Boolean

    /**
     * Performs the mutation on [connection], so calling [isApplied] on the same connection will return `true`.
     */
    fun apply(connection: Connection)

    /**
     * Undoes the database mutation on [connection], so if [isApplied] returned `true` before, it will afterwards return
     * `false`.
     */
    fun undo(connection: Connection)
}
