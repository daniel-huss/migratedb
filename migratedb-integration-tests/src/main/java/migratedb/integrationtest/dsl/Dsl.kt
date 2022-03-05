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
import migratedb.integrationtest.dsl.internal.GivenStepImpl
import migratedb.integrationtest.dsl.internal.ThenStepImpl
import migratedb.integrationtest.dsl.internal.WhenStepImpl
import migratedb.integrationtest.util.container.SharedResources
import org.springframework.jdbc.core.JdbcTemplate

class Dsl(sharedResources: SharedResources) : AutoCloseable {
    private val givenStep = GivenStepImpl(sharedResources)


    override fun close() {
        givenStep.use { }
    }

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
        fun database(dbSystem: DbSystem, block: DatabaseSpec.() -> Unit)
    }

    interface GivenStepResult<G : Any> {
        fun <W : Any> `when`(block: (WhenStep<G>).() -> W): WhenStepResult<G, W>
    }

    interface WhenStep<G> {
        val given: G
        fun migrate(block: RunMigrateSpec.() -> Unit)
    }

    interface WhenStepResult<G : Any, W : Any> {
        fun then(block: (ThenStep<G>).(W) -> Unit)
    }

    interface ThenStep<G : Any> {
        val given: G
        fun withConnection(block: (JdbcTemplate) -> Unit)
    }
}
