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

package migratedb.testing

import io.kotest.assertions.fail
import migratedb.core.api.ClassProvider
import migratedb.core.api.Location
import migratedb.core.api.MigrationPattern
import migratedb.core.api.ResourceProvider
import migratedb.core.api.callback.Callback
import migratedb.core.api.configuration.ClassicConfiguration
import migratedb.core.api.configuration.Configuration
import migratedb.core.api.logging.LogSystem
import migratedb.core.api.migration.JavaMigration
import migratedb.core.api.pattern.ValidatePattern
import migratedb.core.api.resolver.MigrationResolver
import migratedb.core.internal.database.oracle.OracleConfig
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.providers.TypeUsage
import net.jqwik.api.stateful.Action
import net.jqwik.kotlin.api.any
import org.h2.jdbcx.JdbcDataSource
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


object ConfigSetters {
    val baselineDescription = Setter("setBaselineDescription", String.any().ofMaxLength(255))
    val baselineMigrationPrefix = Setter("setBaselineMigrationPrefix", String.any().alpha().ofLength(1))
    val baselineOnMigrate = Setter("setBaselineOnMigrate", Boolean.any())
    val baselineVersion = Setter("setBaselineVersion", anyMigrationVersion())
    val baselineVersionAsString = Setter("setBaselineVersionAsString", anyMigrationVersion().map { it.toString() })
    val batch = Setter("setBatch", Boolean.any())
    val callbacks = Setter("setCallbacks", Arbitraries.ofSuppliers(::MyCallback).array(Callback::class))
    val callbacksAsClassNames = Setter("setCallbacksAsClassNames", Arbitraries.just(MyCallback::class.java.name).array(String::class))
    val cherryPick1 = Setter("setCherryPick", anyMigrationPattern().array(MigrationPattern::class), Array<MigrationPattern>::class)
    val cherryPick2 = Setter("setCherryPick", anyMigrationPattern().map(MigrationPattern::toString).array(String::class), Array<String>::class)
    val cleanDisabled = Setter("setCleanDisabled", Boolean.any())
    val cleanOnValidationError = Setter("setCleanOnValidationError", Boolean.any())
    val connectRetries = Setter("setConnectRetries", Int.any(0..Int.MAX_VALUE))
    val connectRetriesInterval = Setter("setConnectRetriesInterval", Int.any(0..Int.MAX_VALUE))
    val dataSource1 = Setter("setDataSource", Arbitraries.just(JdbcDataSource().apply { setUrl("jdbc:h2:mem:") }), DataSource::class)
    val dataSource2 = Setter("setDataSource", dataSourceParamsArbitrary(), String::class, String::class, String::class)
    val defaultSchema = Setter("setDefaultSchema", anySchemaObjectName())
    val dryRunOutput = Setter("setDryRunOutput", Arbitraries.just(MyOutputStreamFactory()))
    val dryRunOutputAsFileName = Setter("setDryRunOutputAsFileName", Arbitraries.of("target/dryRunOutput1", "target/dryRunOutput2"))
    val encoding = Setter("setEncoding", Arbitraries.of(Charset.availableCharsets().values))
    val encodingAsString = Setter("setEncodingAsString", Arbitraries.of(Charset.availableCharsets().keys))
    val errorOverrides = Setter("setErrorOverrides", anyErrorOverride().array(String::class))
    val failOnMissingLocations = Setter("setFailOnMissingLocations", Boolean.any())
    val group = Setter("setGroup", Boolean.any())
    val ignoreFutureMigrations = Setter("setIgnoreFutureMigrations", Boolean.any())
    val ignoreIgnoredMigrations = Setter("setIgnoreIgnoredMigrations", Boolean.any())
    val ignoreMigrationPatterns1 = Setter("setIgnoreMigrationPatterns", anyValidatePattern().array(ValidatePattern::class), Array<ValidatePattern>::class)
    val ignoreMigrationPatterns2 = Setter("setIgnoreMigrationPatterns", anyValidatePattern().map { it.pattern() }.array(String::class), Array<String>::class)
    val ignoreMissingMigrations = Setter("setIgnoreMissingMigrations", Boolean.any())
    val ignorePendingMigrations = Setter("setIgnorePendingMigrations", Boolean.any())
    val initSql = Setter("setInitSql", String.any())
    val installedBy = Setter("setInstalledBy", String.any())
    val javaMigrationClassProvider = Setter("setJavaMigrationClassProvider", Arbitraries.just(ClassProvider.noClasses<JavaMigration>()))
    val javaMigrations = Setter("setJavaMigrations", Arbitraries.of(MyJavaMigration()).array(JavaMigration::class))
    val jdbcProperties = Setter("setJdbcProperties", anyProperties())
    val licenseKey = Setter("setLicenseKey", String.any())
    val locations = Setter("setLocations", anyLocation().array(Location::class))
    val locationsAsStrings = Setter("setLocationsAsStrings", anyLocation().map { it.toString() }.array(String::class))
    val lockRetryCount = Setter("setLockRetryCount", Int.any(0..Int.MAX_VALUE))
    val logger1 = Setter("setLogger", anyLogSystem(), LogSystem::class)
    val logger2 = Setter("setLogger", anyLogSystemAsString().array(String::class), Array<String>::class)
    val mixed = Setter("setMixed", Boolean.any())
    val outOfOrder = Setter("setOutOfOrder", Boolean.any())
    val outputQueryResults = Setter("setOutputQueryResults", Boolean.any())
    val placeholderPrefix = Setter("setPlaceholderPrefix", String.any().ofMinLength(1))
    val placeholderReplacement = Setter("setPlaceholderReplacement", Boolean.any())
    val placeholders = Setter("setPlaceholders", Arbitraries.maps(String.any(), String.any()))
    val placeholderSuffix = Setter("setPlaceholderSuffix", String.any().ofMinLength(1))
    val repeatableSqlMigrationPrefix = Setter("setRepeatableSqlMigrationPrefix", String.any().ofLength(1))
    val resolvers = Setter("setResolvers", Arbitraries.just(MyMigrationResolver()).array(MigrationResolver::class))
    val resolversAsClassNames = Setter("setResolversAsClassNames", Arbitraries.just(MyMigrationResolver::class.java.name).array(String::class))
    val resourceProvider = Setter("setResourceProvider", Arbitraries.just(ResourceProvider.noResources()))
    val schemas = Setter("setSchemas", anySchemaObjectName().array(String::class).ofMaxSize(10))
    val scriptPlaceholderPrefix = Setter("setScriptPlaceholderPrefix", String.any().ofMinLength(1))
    val scriptPlaceholderSuffix = Setter("setScriptPlaceholderSuffix", String.any().ofMinLength(1))
    val shouldCreateSchemas = Setter("setShouldCreateSchemas", Boolean.any())
    val skipDefaultCallbacks = Setter("setSkipDefaultCallbacks", Boolean.any())
    val skipDefaultResolvers = Setter("setSkipDefaultResolvers", Boolean.any())
    val skipExecutingMigrations = Setter("setSkipExecutingMigrations", Boolean.any())
    val sqlMigrationPrefix = Setter("setSqlMigrationPrefix", String.any().ofMinLength(1))
    val sqlMigrationSeparator = Setter("setSqlMigrationSeparator", String.any().ofMinLength(1))
    val sqlMigrationSuffixes = Setter("setSqlMigrationSuffixes", String.any().ofMinLength(1).array(String::class))
    val table = Setter("setTable", anySchemaObjectName())
    val tablespace = Setter("setTablespace", anySchemaObjectName())
    val target = Setter("setTarget", anyMigrationVersion())
    val targetAsString = Setter("setTargetAsString", anyMigrationVersion().map { it.toString() })
    val undoSqlMigrationPrefix = Setter("setUndoSqlMigrationPrefix", String.any().ofLength(1))
    val validateMigrationNaming = Setter("setValidateMigrationNaming", Boolean.any())
    val validateOnMigrate = Setter("setValidateOnMigrate", Boolean.any())
    val extensionConfig = Setter("setExtensionConfig", extensionConfigParamsArbitrary())

    val all: List<Setter> by lazy {
        ConfigSetters::class.declaredMemberProperties.asSequence()
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                when (it.returnType.classifier) {
                    Setter::class -> (it as KProperty1<ConfigSetters, Setter>).get(this)
                    else -> null
                }
            }
            .toList()
    }

    private fun extensionConfigParamsArbitrary(): Arbitrary<*> {
        val files = String.any().ofMinLength(1).ofMaxLength(100).map { "target/$it" }
        val booleans = Boolean.any()
        val freshConfigs = Arbitraries.ofSuppliers(::OracleConfig)
        return Combinators.combine(freshConfigs, booleans, booleans, files, files).`as` { config, isA, isB, fileA, fileB ->
            config.isOracleSqlplus = isA
            config.isOracleSqlplusWarn = isB
            config.oracleKerberosCacheFile = fileA
            config.oracleKerberosConfigFile = fileB
            Params(arrayOf(config::class.java, config))
        }
    }

    private fun dataSourceParamsArbitrary(): Arbitrary<*> {
        val urls = Arbitraries.just("jdbc:h2:mem:")
        val users = Arbitraries.strings().ofMaxLength(100)
        val passwords = Arbitraries.strings().ofMaxLength(100)
        return Combinators.combine(urls, users, passwords).`as` { url, user, password ->
            Params(arrayOf(url, user, password))
        }
    }

    private class Params(val params: Array<Any>)

    class Setter(name: String, private val validParamsProvider: Arbitrary<*>, vararg signature: KClass<*>) {
        constructor(name: String, validParamsProvider: Arbitrary<*>) : this(
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

        val asAction: Arbitrary<Action<ClassicConfiguration>> =
            validParamsProvider.map { validParam ->
                Action<ClassicConfiguration> {
                    setterFunction(it, validParam)
                    it
                }
            }

        companion object {
            private val setterFunctions = scanMethods().groupBy { it.name }
            private fun scanMethods(): List<SetterFunction> {
                return ClassicConfiguration::class.java.methods
                    .filter {
                        it.name.length > "set".length
                        it.name.startsWith("set") &&
                                !Modifier.isAbstract(it.modifiers) &&
                                !Modifier.isStatic(it.modifiers)
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
