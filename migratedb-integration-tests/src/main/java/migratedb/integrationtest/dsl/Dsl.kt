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

import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.dsl.internal.GivenStepImpl
import migratedb.integrationtest.dsl.internal.ThenStepImpl
import migratedb.integrationtest.dsl.internal.WhenStepImpl
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.SafeIdentifier.Companion.asSafeIdentifier
import migratedb.integrationtest.util.container.SharedResources
import org.springframework.jdbc.core.JdbcTemplate

class Dsl(dbSystem: DbSystem, sharedResources: SharedResources) : AutoCloseable {
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
    fun table(s: SafeIdentifier) = table(s.toString()).asSafeIdentifier()

    /**
     * Normalizes the case of a table name.
     */
    fun table(s: CharSequence): String = databaseHandle.normalizeCase(s)

    fun <G : Any> given(block: (GivenStep).() -> G): GivenStepResult<G> {
        val g = givenStep.block()
        return object : GivenStepResult<G> {
            override fun <W : Any> `when`(block: WhenStep<G>.() -> W): WhenStepResult<G, W> {
                givenStep.executeActions().let { givenInfo ->
                    WhenStepImpl(g, givenInfo).let { whenStep ->
                        val w = whenStep.block()
                        return object : WhenStepResult<G, W> {
                            override fun then(block: (ThenStep<G>).(W) -> Unit) {
                                whenStep.executeActions()
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
        fun migrate(block: RunMigrateSpec.() -> Unit)
        fun arbitraryMutation(): IndependentDatabaseMutation
    }

    interface WhenStepResult<G : Any, W : Any> {
        fun then(block: (ThenStep<G>).(W) -> Unit)
    }

    interface ThenStep<G : Any> : AfterGiven<G> {
        fun withConnection(block: (JdbcTemplate) -> Unit)
    }
}
