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

package migratedb.integrationtest.database

import migratedb.core.internal.database.DatabaseType
import migratedb.integrationtest.SafeIdentifier
import migratedb.integrationtest.SharedResources
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.*
import java.util.stream.Stream
import javax.sql.DataSource

interface DatabaseSupport {
    interface Handle : AutoCloseable {
        val type: DatabaseType

        /**
         * Creates a database and returns a connection to that database (with admin privileges).
         */
        fun createDatabaseIfNotExists(dbName: SafeIdentifier): DataSource

        /**
         * Drops an existing database.
         */
        fun dropDatabaseIfExists(dbName: SafeIdentifier)

        /**
         * Connects to a database/schema combination with administrative privileges.
         */
        fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource

        /**
         * Generates a new database mutation that is independent from all previously generated database mutations.
         */
        fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation
    }

    fun get(sharedResources: SharedResources): Handle


    class All : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
            MariaDb.values(),
            Postgres.values(),
        ).flatMap { Arrays.stream(it) }.map { Arguments.arguments(it) }
    }
}
