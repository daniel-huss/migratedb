package migratedb.integrationtest.parser

import io.kotest.matchers.shouldBe
import migratedb.core.api.resource.Resource
import migratedb.integrationtest.database.*
import migratedb.integrationtest.util.base.IntegrationTest
import migratedb.integrationtest.util.dsl.RunMigrateSpec

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
                ActionCategory.Create -> values()
                ActionCategory.Drop -> values().apply { reverse() }
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
            is Db2 -> "db2"
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
                val expectedMigrationCount = ComponentCategory.values().size * when (skipDrop) {
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
                fromScript("V${migrationCount}__$description", scriptResource.readText())
            }
        }
    }
}
