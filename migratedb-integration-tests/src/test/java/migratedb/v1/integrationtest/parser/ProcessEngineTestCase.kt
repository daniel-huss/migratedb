/*
 * Copyright 2022-2024 The MigrateDB contributors
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

package migratedb.v1.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.v1.core.api.resource.Resource
import migratedb.v1.integrationtest.database.*
import migratedb.v1.integrationtest.util.base.IntegrationTest
import migratedb.v1.integrationtest.util.dsl.RunMigrateSpec

/**
 * Parser test case that checks whether the non-trivial database schema create/drop scripts of some process engine can
 * be parsed and executed.
 */
class ProcessEngineTestCase(val dbSystem: DbSystem, val skipDrop: Boolean = false) : IntegrationTest() {
    private enum class ComponentCategory(private val resourceNamePart: String) {
        Identity("identity"),
        Engine("engine"),
        History("history"),
        CaseEngine("case.engine"),
        CaseHistory("case.history"),
        DecisionEngine("decision.engine");

        override fun toString() = resourceNamePart

        companion object {
            fun inOrder(action: ActionCategory) = when (action) {
                ActionCategory.Create -> entries
                ActionCategory.Drop -> entries.asReversed()
            }
        }
    }

    private enum class ActionCategory(val resourceNamePart: String) {
        Create("create"), Drop("drop");

        override fun toString() = resourceNamePart
    }

    private fun resourceName(dbSystem: DbSystem, component: ComponentCategory, action: ActionCategory) =
        "/org/camunda/bpm/engine/db/$action/activiti." + when (dbSystem) {
            is CockroachDb -> "cockroachdb"
            is MariaDb -> "mariadb"
            is SqlServer -> "mssql"
            is MySql -> "mysql"
            is Oracle -> "oracle"
            is Postgres -> "postgres"
            is H2 -> "h2"
            else -> throw IllegalArgumentException("No script for $dbSystem")
        } + ".$action.$component.sql"

    init {
        withDsl(dbSystem) {
            given {
                database { }
            }.`when` {
                migrate {
                    createThenDropAllComponents()
                }
            }.then {
                val expectedMigrationCount = ComponentCategory.entries.size * when (skipDrop) {
                    true -> 1
                    false -> 2
                }
                it.migrationsExecuted.shouldBe(expectedMigrationCount)
            }
        }
    }

    private fun RunMigrateSpec.createThenDropAllComponents() {
        var migrationCount = 0
        val actions = when (skipDrop) {
            true -> listOf(ActionCategory.Create)
            false -> listOf(ActionCategory.Create, ActionCategory.Drop)
        }
        actions.forEach { action ->
            ComponentCategory.inOrder(action).forEach { component ->
                val scriptResourceName = resourceName(dbSystem, component, action)
                val description = Resource.lastNameComponentOf(scriptResourceName)
                val scriptResource = this::class.java.getResource(scriptResourceName)
                    ?: throw IllegalStateException("No such resource: $scriptResourceName")
                migrationCount++
                usingScript("V${migrationCount}__$description", scriptResource.readText())
            }
        }
    }
}
