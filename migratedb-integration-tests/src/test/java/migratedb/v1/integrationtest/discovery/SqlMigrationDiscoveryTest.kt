/*
 * Copyright 2022-2025 The MigrateDB contributors
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

package migratedb.v1.integrationtest.discovery

import io.kotest.inspectors.shouldForExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import migratedb.v1.core.api.Location
import migratedb.v1.core.api.MigrationType
import migratedb.v1.core.api.Version
import migratedb.v1.core.api.configuration.DefaultConfiguration
import migratedb.v1.core.internal.parser.ParsingContextImpl
import migratedb.v1.core.internal.resolver.sql.SqlMigrationResolver
import migratedb.v1.core.internal.util.LocationScanner
import org.junit.jupiter.api.Test

internal class SqlMigrationDiscoveryTest {

    @Test
    fun `Versioned, repeatable and baseline migrations are found by classpath index scanning`() {
        // Given
        val discovery = newSqlMigrationResolver()

        // When
        val resolved = discovery.resolveMigrations { DefaultConfiguration() }

        // Then
        resolved.shouldHaveSize(3)
        resolved.shouldForExactly(1) {
            it.type.shouldBeEqual(MigrationType.SQL)
            it.version.shouldBeEqual(Version.parse("000"))
            it.description.shouldBeEqual("Versioned")
        }
        resolved.shouldForExactly(1) {
            it.type.shouldBeEqual(MigrationType.SQL_BASELINE)
            it.version.shouldBeEqual(Version.parse("001"))
            it.description.shouldBeEqual("Baseline")
        }
        resolved.shouldForExactly(1) {
            it.type.shouldBeEqual(MigrationType.SQL)
            it.isRepeatable.shouldBeEqual(true)
            it.version.shouldBeNull()
            it.description.shouldBeEqual("Repeatable")
        }
    }

    private fun newSqlMigrationResolver(): SqlMigrationResolver {
        return SqlMigrationResolver(
            LocationScanner(
                Void::class.java,
                listOf(Location.ClassPathLocation("classpath-migrations", null)),
                false
            ),
            { _, _ -> null },
            { _, _, _ -> null },
            DefaultConfiguration(),
            ParsingContextImpl()
        )
    }
}
