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

class Dsl(private val sharedResources: SharedResources) {

    fun <G : Any> given(block: (GivenStep).() -> G): GivenStepResult<G> {
        val givenStep = GivenStep(sharedResources)
        val givenResult = givenStep.block()
        return object : GivenStepResult<G> {
            override fun <W : Any> `when`(block: WhenStep<G>.() -> W): WhenStepResult<G, W> {
                val whenStep = WhenStep(givenResult)
                givenStep.beforeWhen()
                val whenResult = whenStep.block()
                return object : WhenStepResult<G, W> {
                    override fun then(block: (ThenStep<G>).(W) -> Unit) {
                        val thenStep = ThenStep(givenResult)
                        val allSteps = listOf(givenStep, whenStep)
                        try {
                            allSteps.forEach { it.beforeThen() }
                            thenStep.block(whenResult)
                        } finally {
                            allSteps.forEach {
                                runCatching { it.cleanup() }
                                    .exceptionOrNull()
                                    ?.takeIf { it is InterruptedException }
                                    ?.let { Thread.currentThread().interrupt() }
                            }
                        }
                    }
                }
            }
        }
    }
}
