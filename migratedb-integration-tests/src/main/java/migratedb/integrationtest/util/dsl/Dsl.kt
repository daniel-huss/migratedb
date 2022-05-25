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

package migratedb.integrationtest.util.dsl

import migratedb.core.api.Checksum
import migratedb.core.api.MigrationInfo
import migratedb.core.api.MigrationInfoService
import migratedb.core.api.Version
import migratedb.core.api.internal.schemahistory.AppliedMigration
import migratedb.core.api.output.MigrateResult
import migratedb.core.api.output.RepairResult
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.container.SharedResources
import migratedb.integrationtest.util.dsl.internal.GivenStepImpl
import migratedb.integrationtest.util.dsl.internal.ThenStepImpl
import migratedb.integrationtest.util.dsl.internal.WhenStepImpl
import org.springframework.jdbc.core.JdbcTemplate

class Dsl(dbSystem: DbSystem, sharedResources: SharedResources) : AutoCloseable {
    companion object {
        /**
         * Auto-completes a shortened migration name like "V1" to a valid migration name like "V1__V1".
         */
        fun String.toMigrationName() = when {
            contains("__") -> this
            else -> "${this}__V${this.drop(1)}"
        }

        /**
         * Reconstructs the migration name in the default format, e.g., "V1__My_Cool_Migration".
         */
        fun MigrationInfo.migrationName() = when {
            isRepeatable -> "R__" + description.spaceToUnderscore()
            else -> (if (type.isBaselineMigration) "B" else "V") +
                    version?.dotToUnderscrore() + "__" + description.spaceToUnderscore()
        }

        /**
         * Returns a checksum that depends on the string value and the [delta] value. If, and only if, [delta] is zero, the
         * checksum will be equal to `Checksum.builder().addString(this).build()`.
         */
        fun String.checksum(delta: Int = 0): Checksum = Checksum.builder()
            .addString(this).apply {
                delta.takeUnless { it == 0 }?.let {
                    addNumber(it.toBigInteger())
                }
            }
            .build()

        private fun Version.dotToUnderscrore() = toString().replace('.', '_')
        private fun String.spaceToUnderscore() = replace(' ', '_')

        fun List<String>.toMigrationNames() = this.map { it.toMigrationName() }

        /**
         * Applies [toMigrationName] to every element in a list of strings, returning `null` if this list is `null`.
         */
        @JvmName("addDescriptionIfMissingOrNull")
        fun List<String>?.toMigrationNames() = this?.map { it.toMigrationName() }
    }

    private val databaseHandle = dbSystem.get(sharedResources)
    private val givenStep = GivenStepImpl(databaseHandle)

    override fun close() {
        databaseHandle.use {
            givenStep.use { }
        }
    }

    /**
     * Normalizes the case of a table name.
     */
    fun normalize(s: SafeIdentifier) = normalize(s.toString()).asSafeIdentifier()

    /**
     * Normalizes the case of a table name.
     */
    fun normalize(s: CharSequence): String = databaseHandle.normalizeCase(s)

    fun <G : Any> given(block: (GivenStep).() -> G): GivenStepResult<G> {
        val g = givenStep.block()
        return object : GivenStepResult<G> {
            override fun <W : Any> `when`(block: WhenStep<G>.() -> W): WhenStepResult<G, W> {
                givenStep.executeActions().let { givenInfo ->
                    WhenStepImpl(g, givenInfo).let { whenStep ->
                        val w = whenStep.block()
                        return object : WhenStepResult<G, W> {
                            override fun then(block: (ThenStep<G>).(W) -> Unit) {
                                val thenStep = ThenStepImpl(g, givenInfo)
                                thenStep.block(w)
                            }
                        }
                    }
                }
            }
        }
    }

    interface GivenStep {
        fun database(block: DatabaseSpec.() -> Unit)
    }

    interface GivenStepResult<G : Any> {
        fun <W : Any> `when`(block: (WhenStep<G>).() -> W): WhenStepResult<G, W>
    }

    interface AfterGiven<G> : QualifiedTableNameProvider {
        val given: G
        val schemaName: SafeIdentifier?
    }

    interface WhenStep<G> : AfterGiven<G> {
        fun migrate(block: RunMigrateSpec.() -> Unit): MigrateResult
        fun info(block: RunInfoSpec.() -> Unit): MigrationInfoService

        fun repair(block: RunRepairSpec.() -> Unit): RepairResult
        fun arbitraryMutation(): IndependentDatabaseMutation
    }

    interface WhenStepResult<G : Any, W : Any> {
        fun then(block: (ThenStep<G>).(W) -> Unit)
    }

    interface ThenStep<G : Any> : AfterGiven<G> {
        fun withConnection(block: (JdbcTemplate) -> Unit)

        fun schemaHistory(table: String? = null, block: (List<AppliedMigration>).() -> Unit)
    }
}
