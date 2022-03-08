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
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.container.SharedResources
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.*
import java.util.stream.Stream
import javax.sql.DataSource

/**
 * Adapter for the various supported database systems.
 */
interface DbSystem {
    interface Handle : AutoCloseable {
        val type: DatabaseType

        /**
         * Creates the schema or database with the given name if it doesn't exist. Some database systems don't support
         * either schema-level or database-level isolation, so it's undefined whether the namespace will be implemented
         * at the schema or at the database level.
         *
         * @return The identifier that must be used to qualify items in the namespace.
         */
        fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier

        /**
         * Drops an existing schema or database.
         */
        fun dropNamespaceIfExists(namespace: SafeIdentifier)

        /**
         * Connects to the database with administrative privileges.
         */
        fun newAdminConnection(namespace: SafeIdentifier): DataSource

        /**
         * Generates a new database mutation within [namespace] that is independent from all previously generated
         * database mutations in the same namespace.
         */
        fun nextMutation(namespace: SafeIdentifier): IndependentDatabaseMutation

        /**
         * Normalizes the case of an identifier.
         */
        fun normalizeCase(s: CharSequence): String = s.toString().uppercase()
    }

    fun get(sharedResources: SharedResources): Handle


    class All : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
            Db2.values(),
            MariaDb.values(),
            SqlServer.values(),
            MySql.values(),
            Oracle.values(),
            Postgres.values(),
            Sqlite.values(),
        ).flatMap { Arrays.stream(it) }.map { Arguments.arguments(it) }
    }
}
