package migratedb.v1.core.testing

import io.kotest.assertions.fail
import migratedb.v1.core.api.ClassProvider
import migratedb.v1.core.api.Location
import migratedb.v1.core.api.MigrationPattern
import migratedb.v1.core.api.ResourceProvider
import migratedb.v1.core.api.callback.Callback
import migratedb.v1.core.api.configuration.Configuration
import migratedb.v1.core.api.configuration.DefaultConfiguration
import migratedb.v1.core.api.logging.LogSystem
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.core.api.pattern.ValidatePattern
import migratedb.v1.core.api.resolver.MigrationResolver
import migratedb.v1.core.internal.database.oracle.OracleConfig
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitraries.just
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.providers.TypeUsage
import net.jqwik.api.stateful.Action
import net.jqwik.kotlin.api.any
import net.jqwik.kotlin.api.ofLength
import org.h2.jdbcx.JdbcDataSource
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.jvmErasure


@Suppress("unused") // Props are used via reflection
object ConfigSetters {
    val baselineDescription = Setter("setBaselineDescription", String.any().ofLength(1..100))
    val baselineMigrationPrefix = Setter("setBaselineMigrationPrefix", String.any().alpha().ofLength(1))
    val baselineOnMigrate = Setter("setBaselineOnMigrate", Boolean.any())
    val baselineVersion = Setter("setBaselineVersion", anyMigrationVersion())
    val baselineVersionAsString = Setter("setBaselineVersionAsString", anyMigrationVersionString())
    val callbacks1 = Setter(
        "setCallbacks",
        just(UniversalDummy()).array(Callback::class),
        Array<Callback>::class
    )
    val callbacks2 = Setter(
        "setCallbacks",
        just(UniversalDummy()).list(),
        Collection::class
    )
    val callbacksAsClassNames1 = Setter(
        "setCallbacksAsClassNames",
        just(UniversalDummy::class.java.name).array(String::class),
        Array<String>::class
    )
    val callbacksAsClassNames2 = Setter(
        "setCallbacksAsClassNames",
        just(UniversalDummy::class.java.name).list(),
        Collection::class
    )
    val cherryPick1 = Setter(
        "setCherryPick",
        anyMigrationPattern().array(MigrationPattern::class),
        Array<MigrationPattern>::class
    )
    val cherryPick2 = Setter(
        "setCherryPick",
        anyMigrationPattern().list(),
        Collection::class
    )
    val cherryPickAsString1 = Setter(
        "setCherryPickAsString",
        anyMigrationPattern().map(MigrationPattern::toString).array(String::class),
        Array<String>::class
    )
    val cherryPickAsString2 = Setter(
        "setCherryPickAsString",
        anyMigrationPattern().map(MigrationPattern::toString).list(),
        Collection::class
    )
    val connectRetries = Setter("setConnectRetries", Int.any(0..Int.MAX_VALUE))
    val connectRetriesInterval = Setter("setConnectRetriesInterval", Int.any(0..Int.MAX_VALUE))
    val dataSource = Setter("setDataSource", just(JdbcDataSource().apply { setUrl("jdbc:h2:mem:") }), DataSource::class)
    val defaultSchema = Setter("setDefaultSchema", anySchemaObjectName())
    val encoding = Setter("setEncoding", Arbitraries.of(Charset.availableCharsets().values))
    val encodingAsString = Setter("setEncodingAsString", Arbitraries.of(Charset.availableCharsets().keys))
    val failOnMissingLocations = Setter("setFailOnMissingLocations", Boolean.any())
    val group = Setter("setGroup", Boolean.any())
    val ignoreFutureMigrations = Setter("setIgnoreFutureMigrations", Boolean.any())
    val ignoreIgnoredMigrations = Setter("setIgnoreIgnoredMigrations", Boolean.any())
    val ignoreMigrationPatterns1 = Setter(
        "setIgnoreMigrationPatterns",
        anyValidatePattern().array(ValidatePattern::class),
        Array<ValidatePattern>::class
    )
    val ignoreMigrationPatterns2 = Setter(
        "setIgnoreMigrationPatterns",
        anyValidatePattern().list(),
        Collection::class
    )
    val ignoreMigrationPatternsAsStrings1 = Setter(
        "setIgnoreMigrationPatternsAsStrings",
        anyValidatePattern().map { it.pattern() }.array(String::class),
        Array<String>::class
    )
    val ignoreMigrationPatternsAsStrings2 = Setter(
        "setIgnoreMigrationPatternsAsStrings",
        anyValidatePattern().map { it.pattern() }.list(),
        Collection::class
    )
    val ignoreMissingMigrations = Setter("setIgnoreMissingMigrations", Boolean.any())
    val ignorePendingMigrations = Setter("setIgnorePendingMigrations", Boolean.any())
    val initSql = Setter("setInitSql", String.any().ofMaxLength(100))
    val installedBy = Setter("setInstalledBy", String.any().ofMaxLength(100))
    val javaMigrationClassProvider = Setter(
        "setJavaMigrationClassProvider",
        just(ClassProvider.noClasses<JavaMigration>())
    )
    val javaMigrations1 = Setter(
        "setJavaMigrations",
        just(UniversalDummy()).array(JavaMigration::class),
        Array<JavaMigration>::class
    )
    val javaMigrations2 = Setter(
        "setJavaMigrations",
        just(UniversalDummy()).list(),
        Collection::class
    )
    val locations1 = Setter(
        "setLocations",
        anyLocation().array(Location::class),
        Array<Location>::class
    )
    val locations2 = Setter(
        "setLocations",
        anyLocation().list(),
        Collection::class
    )
    val locationsAsStrings1 = Setter(
        "setLocationsAsStrings",
        anyLocation().map { it.toString() }.array(String::class),
        Array<String>::class
    )
    val locationsAsStrings2 = Setter(
        "setLocationsAsStrings",
        anyLocation().map { it.toString() }.list(),
        Collection::class
    )
    val lockRetryCount = Setter("setLockRetryCount", Int.any(0..Int.MAX_VALUE))
    val logger1 = Setter("setLogger", anyLogSystem(), LogSystem::class)
    val logger2 = Setter("setLogger", anyLogSystemAsString().array(String::class), Array<String>::class)
    val mixed = Setter("setMixed", Boolean.any())
    val outOfOrder = Setter("setOutOfOrder", Boolean.any())
    val outputQueryResults = Setter("setOutputQueryResults", Boolean.any())
    val placeholderPrefix = Setter("setPlaceholderPrefix", String.any().ofLength(1..100))
    val placeholderReplacement = Setter("setPlaceholderReplacement", Boolean.any())
    val placeholders = Setter(
        "setPlaceholders", Arbitraries.maps(String.any().ofLength(0..100), String.any().ofLength(1..100))
    )
    val placeholderSuffix = Setter("setPlaceholderSuffix", String.any().ofLength(1..100))
    val repeatableSqlMigrationPrefix = Setter("setRepeatableSqlMigrationPrefix", String.any().ofLength(1))
    val resolvers1 = Setter(
        "setResolvers",
        just(UniversalDummy()).array(MigrationResolver::class),
        Array<MigrationResolver>::class
    )
    val resolvers2 = Setter(
        "setResolvers",
        just(UniversalDummy()).list(),
        Collection::class
    )
    val resolversAsClassNames1 = Setter(
        "setResolversAsClassNames",
        just(UniversalDummy::class.java.name).array(String::class),
        Array<String>::class
    )
    val resolversAsClassNames2 = Setter(
        "setResolversAsClassNames",
        just(UniversalDummy::class.java.name).list(),
        Collection::class
    )
    val resourceProvider = Setter("setResourceProvider", just(ResourceProvider.noResources()))
    val schemas1 = Setter(
        "setSchemas",
        anySchemaObjectName().array(String::class).ofMaxSize(10),
        Array<String>::class
    )
    val schemas2 = Setter(
        "setSchemas",
        anySchemaObjectName().list().ofMaxSize(10),
        Collection::class
    )
    val scriptPlaceholderPrefix = Setter("setScriptPlaceholderPrefix", String.any().ofLength(1..10))
    val scriptPlaceholderSuffix = Setter("setScriptPlaceholderSuffix", String.any().ofLength(1..10))
    val createSchemas = Setter("setCreateSchemas", Boolean.any())
    val skipDefaultCallbacks = Setter("setSkipDefaultCallbacks", Boolean.any())
    val skipDefaultResolvers = Setter("setSkipDefaultResolvers", Boolean.any())
    val skipExecutingMigrations = Setter("setSkipExecutingMigrations", Boolean.any())
    val sqlMigrationPrefix = Setter("setSqlMigrationPrefix", String.any().ofLength(1))
    val sqlMigrationSeparator = Setter("setSqlMigrationSeparator", String.any().ofLength(1..10))
    val sqlMigrationSuffixes1 = Setter(
        "setSqlMigrationSuffixes",
        String.any().ofLength(1..10).array(String::class),
        Array<String>::class
    )
    val sqlMigrationSuffixes2 = Setter(
        "setSqlMigrationSuffixes",
        String.any().ofLength(1..10).list(),
        Collection::class
    )
    val table = Setter("setTable", anySchemaObjectName())
    val oldTable = Setter("setOldTable", anySchemaObjectName())
    val liberateOnMigrate = Setter("setLiberateOnMigrate", Boolean.any())
    val tablespace = Setter("setTablespace", anySchemaObjectName())
    val target = Setter("setTarget", anyTargetVersion())
    val targetAsString = Setter("setTargetAsString", anyTargetVersionString())
    val validateMigrationNaming = Setter("setValidateMigrationNaming", Boolean.any())
    val validateOnMigrate = Setter("setValidateOnMigrate", Boolean.any())
    val extensionConfig = Setter("setExtensionConfig", extensionConfigParamsArbitrary())

    val all: List<Setter> by lazy {
        ConfigSetters::class.declaredMemberProperties.asSequence()
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                when (it.returnType.jvmErasure) {
                    Setter::class -> (it as KProperty1<ConfigSetters, Setter>).get(this)
                    else -> null
                }
            }
            .toList()
    }

    private fun extensionConfigParamsArbitrary(): Arbitrary<out Any> {
        val files = String.any().ofLength(1..100).map { "target/$it" }
        val booleans = Boolean.any()
        val freshConfigs = Arbitraries.ofSuppliers(::OracleConfig)
        return Combinators.combine(freshConfigs, booleans, booleans, files, files, files)
            .`as` { oracle, isA, isB, fileA, fileB, fileC ->
                oracle.isSqlplus = isA
                oracle.isSqlplusWarn = isB
                oracle.kerberosCacheFile = fileA
                oracle.kerberosConfigFile = fileB
                oracle.walletLocation = fileC
                Params(arrayOf(oracle::class.java, oracle))
            }
    }

    /**
     * Used only if a setter accepts more than one parameter.
     */
    private class Params(val params: Array<Any>)

    class Setter(name: String, private val validParamsProvider: Arbitrary<out Any>, vararg signature: KClass<*>) {
        constructor(name: String, validParamsProvider: Arbitrary<out Any>) : this(
            name = name,
            signature = emptyArray(),
            validParamsProvider = validParamsProvider
        )

        private val setterFunction = when {
            signature.isEmpty() -> setterFunctions.getValue(name).singleOrNull()
                ?: throw IllegalArgumentException("There are multiple signatures for setter $name")

            else -> setterFunctions.getValue(name).singleOrNull { it.matches(signature) }
                ?: throw IllegalArgumentException("No setter named $name matches signature ${signature.map { it.simpleName }}")
        }

        val asAction: Arbitrary<Action<DefaultConfiguration>>
            get() =
                validParamsProvider.map { validParam: Any ->
                    Action<DefaultConfiguration> {
                        setterFunction(it, validParam)
                        it
                    }
                }

        companion object {
            private val setterFunctions = scanMethods().groupBy { it.name }

            private fun scanMethods(): List<SetterFunction> {
                return DefaultConfiguration::class.java.methods
                    .filter {
                        it.name.length > "set".length
                        it.name.startsWith("set") &&
                                !Modifier.isAbstract(it.modifiers) &&
                                !Modifier.isStatic(it.modifiers) &&
                                !it.isDefault
                    }.map {
                        SetterFunction(
                            name = it.name,
                            argTypes = it.genericParameterTypes.map(TypeUsage::forType),
                            method = it
                        )
                    }
                    .toList()
            }
        }

        internal class SetterFunction(
            val name: String,
            private val argTypes: List<TypeUsage>,
            private val method: Method
        ) : (Configuration, Any) -> Unit {

            override fun invoke(p1: Configuration, p2: Any) {
                try {
                    when (p2) {
                        is Params -> method.invoke(p1, *p2.params)
                        else -> method.invoke(p1, p2)
                    }
                } catch (e: IllegalArgumentException) {
                    fail("Cannot call $method with parameters $p2")
                }
            }

            fun matches(signature: Array<out KClass<*>>): Boolean {
                if (argTypes.size != signature.size) {
                    return false
                }
                signature.forEachIndexed { index, clazz ->
                    if (!argTypes[index].isAssignableFrom(clazz.java)) {
                        return false
                    }
                }
                return true
            }
        }
    }
}
