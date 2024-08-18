package migratedb.v1.integrationtest.util.dsl

import migratedb.v1.integrationtest.database.*
import migratedb.v1.testing.util.base.Args
import org.flywaydb.core.api.MigrationType
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.callback.NoopCallbackExecutor
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory
import org.flywaydb.core.internal.logging.multi.MultiLogger
import org.flywaydb.core.internal.parser.ParsingContext
import org.flywaydb.core.internal.schemahistory.AppliedMigration
import org.flywaydb.core.internal.schemahistory.SchemaHistoryFactory
import kotlin.random.Random

class DatabasesSupportedByFw : Args(
    functionHasOneParameter = OneParam.YES,
    *databases()
) {
    companion object {
        fun databases() = arrayOf(
            Sqlite.V3_8_11_2,
            Sqlite.V3_36_0_3,
            Derby.V10_15_2_0,
            MariaDb.V10_6,
            SqlServer.V2019_CU15,
            Hsqldb.V2_6_1,
            Postgres.V14,
            H2.V2_1_210,
            Informix.V14_10,
        )

        init {
            // Silence!
            LogFactory.setLogCreator { MultiLogger(emptyList()) }
        }
    }
}

//
interface FwSchemaHistorySpec {
    fun entry(version: String?, description: String, type: String, success: Boolean)
}

fun Dsl.GivenStep.fwSchemaHistory(
    table: String,
    block: (FwSchemaHistorySpec).() -> Unit
): List<AppliedMigration> {
    val result = mutableListOf<AppliedMigration>()
    extend { databaseContext ->
        val config = FluentConfiguration()
            .dataSource(databaseContext.adminDataSource)
            .table(table)
            .apply { databaseContext.schemaName?.let { schemas("$it") } }
        FwSchemaHistory(config, Random(seed = 0)).use { schemaHistory ->
            schemaHistory.create()
            object : FwSchemaHistorySpec {
                override fun entry(version: String?, description: String, type: String, success: Boolean) {
                    schemaHistory.add(version, description, type, "script", 0, 0, success)
                }
            }.block()
            result.addAll(schemaHistory.get())
        }
    }
    return result
}

fun Dsl.GivenStep.fwSchemaHistory(table: String, size: Int): List<AppliedMigration> {
    val result = mutableListOf<AppliedMigration>()
    extend { databaseContext ->
        val config = FluentConfiguration()
            .dataSource(databaseContext.adminDataSource)
            .table(table)
            .apply { databaseContext.schemaName?.let { schemas("$it") } }
        FwSchemaHistory(config, Random(seed = size)).use { schemaHistory ->
            schemaHistory.create()
            val remaining = size - schemaHistory.get().size
            repeat(remaining.coerceAtLeast(0)) {
                schemaHistory.addRandom()
            }
            result.addAll(schemaHistory.get())
        }
    }
    return result
}


class FwSchemaHistory(private val configuration: FluentConfiguration, private val random: Random) : AutoCloseable {
    private val jdbcConnectionFactory = JdbcConnectionFactory(configuration.dataSource, configuration, null)
    private val database = jdbcConnectionFactory.databaseType.createDatabase(
        configuration,
        false,
        jdbcConnectionFactory,
        null
    )
    private val parsingContext = ParsingContext()
    private val sqlScriptFactory =
        jdbcConnectionFactory.databaseType.createSqlScriptFactory(configuration, parsingContext)
    private val noCallbackSqlScriptExecutorFactory =
        jdbcConnectionFactory.databaseType.createSqlScriptExecutorFactory(
            jdbcConnectionFactory,
            NoopCallbackExecutor.INSTANCE,
            null
        )
    private val schemas = SchemaHistoryFactory.prepareSchemas(configuration, database)
    private val defaultSchema = schemas.left
    private val delegate =
        SchemaHistoryFactory.getSchemaHistory(
            configuration,
            noCallbackSqlScriptExecutorFactory,
            sqlScriptFactory,
            database,
            defaultSchema,
            null
        )
    private val versionedTypes = listOf(
        "BASELINE",
        "CUSTOM",
        "UNDO_CUSTOM",
        "UNDO_JDBC",
        "UNDO_SQL",
        "UNDO_SPRING_JDBC",
        "UNDO_SCRIPT",
        "DELETE",
        "JDBC",
        "JDBC_STATE_SCRIPT",
        "SQL",
        "SQL_STATE_SCRIPT",
        "SCRIPT",
        "SPRING_JDBC",
        "SCRIPT_BASELINE",
        "JDBC_BASELINE",
        "SQL_BASELINE"
    )
    private val repeatableTypes = listOf(
        "SQL",
        "SCRIPT",
        "JDBC",
        "SPRING_JDBC",
        "DELETE",
        "CUSTOM",
    )

    fun addRandom() {
        val version: String?
        val type: String
        when (random.nextBoolean()) {
            true -> {     // versioned
                version = random.nextInt(1, Int.MAX_VALUE).toString()
                type = versionedTypes.random(random)
            }

            false -> {    // repeatable
                version = null
                type = repeatableTypes.random(random)
            }
        }
        val description = randomDescription()
        val script = "script" + random.nextInt()
        val checksum = when (random.nextBoolean()) {
            true -> random.nextInt()
            false -> null
        }
        add(version, description, type, script, checksum, random.nextInt(), random.nextBoolean())
    }

    private fun randomDescription(): String {
        return when (random.nextInt(4)) {
            0 -> ""
            1 -> "D"
            2 -> "__"
            else -> "123"
        }
    }

    fun add(
        version: String?,
        description: String,
        type: String,
        script: String,
        checksum: Int?,
        executionTime: Int,
        success: Boolean
    ) {
        delegate.addAppliedMigration(
            version?.let { MigrationVersion.fromVersion(it) },
            description,
            MigrationType.CUSTOM,
            script,
            checksum,
            executionTime,
            success
        )
        val table = defaultSchema.getTable(configuration.table)
        val justInserted = get().last().installedRank
        database.mainConnection.jdbcTemplate.update(
            "update $table set ${database.quote("type")} = ?" +
                " where ${database.quote("installed_rank")}  = ?",
            type, justInserted
        )
    }

    fun create() {
        delegate.create(false)
    }

    fun get(): List<AppliedMigration> = delegate.allAppliedMigrations()


    override fun close() {
        database.use { }
    }
}
