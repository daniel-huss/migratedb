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

import migratedb.core.api.internal.database.base.DatabaseType
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.container.SharedResources
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.*
import java.util.stream.Stream
import javax.sql.DataSource

interface DbSystem {
    interface Handle : AutoCloseable {
        val type: DatabaseType

        /**
         * Creates a database and returns a connection to that database (with admin privileges).
         */
        fun createDatabaseIfNotExists(databaseName: SafeIdentifier): DataSource

        /**
         * Drops an existing database.
         */
        fun dropDatabaseIfExists(databaseName: SafeIdentifier)

        /**
         * Connects to a database/schema combination with administrative privileges.
         */
        fun newAdminConnection(databaseName: SafeIdentifier, schemaName: SafeIdentifier): DataSource

        /**
         * Generates a new database mutation that is independent from all previously generated database mutations.
         */
        fun nextMutation(schemaName: SafeIdentifier): IndependentDatabaseMutation

        /**
         * Creates the schema with the given name if the schema doesn't exist. Returns [schemaName] if the schema exists
         * or has been created. Returns `null` if the database system does not support schemas.
         */
        fun createSchemaIfNotExists(databaseName: SafeIdentifier, schemaName: SafeIdentifier): SafeIdentifier?
    }

    fun get(sharedResources: SharedResources): Handle


    class All : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
            MariaDb.values(),
            Postgres.values(),
            Sqlite.values(),
        ).flatMap { Arrays.stream(it) }.map { Arguments.arguments(it) }
    }
}
