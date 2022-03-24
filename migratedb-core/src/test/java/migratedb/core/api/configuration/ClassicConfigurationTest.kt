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
package  migratedb.core.api.configuration

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldBeEmpty
import migratedb.core.api.Location
import migratedb.core.api.MigrateDbException
import migratedb.core.api.Version
import migratedb.core.api.configuration.PropertyNames.ErrorOverrideString
import migratedb.core.api.configuration.PropertyNames.Info
import migratedb.core.api.configuration.PropertyNames.JdbcUrlString
import migratedb.core.api.pattern.ValidatePattern
import migratedb.core.testing.MigrateDbDomain
import migratedb.core.testing.UniversalDummy
import migratedb.core.testing.anyErrorOverride
import migratedb.core.testing.anyLocation
import migratedb.core.testing.anyMigrationVersionString
import migratedb.core.testing.anyValidatePattern
import migratedb.core.testing.comparableProperties
import migratedb.core.testing.diffWith
import migratedb.core.testing.withArbitrariesOutsideOfProperty
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitraries.just
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.domains.Domain
import net.jqwik.kotlin.api.any
import net.jqwik.kotlin.api.ofLength
import net.jqwik.kotlin.api.ofSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Stream

@Domain(MigrateDbDomain::class)
internal class ClassicConfigurationTest {
    @Property(tries = 200)
    fun `configure(Configuration) copies all properties except class loader into empty configuration`(
        @ForAll source: ClassicConfiguration
    ) {
        // when
        val target = ClassicConfiguration()
        target.configure(source)

        // then
        target.comparableProperties().diffWith(source.comparableProperties()).asClue {
            it.shouldBeEmpty()
        }
    }

    @Property(tries = 200)
    fun `configure(Configuration) copies all properties except class loader into non-empty configuration`(
        @ForAll source: ClassicConfiguration,
        @ForAll target: ClassicConfiguration
    ) {
        // when
        target.configure(source)

        // then
        target.comparableProperties().diffWith(source.comparableProperties()).asClue {
            it.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ConfigKeyFields::class)
    fun `configure(Properties) supports all config keys`(field: ConfigKeyField) {
        shouldNotThrowAny {
            ClassicConfiguration().configure(field.sampleProperties())
        }
    }

    @Test
    fun `configure(Properties) throws if there are unsupported config keys`() {
        shouldThrow<MigrateDbException> {
            ClassicConfiguration().configure(
                mapOf(
                    "migratedb.group" to "true",
                    "migratedb.unsupportedThingy" to "somehing"
                )
            )
        }
    }

    data class ConfigKeyField(val field: Field) {
        override fun toString(): String {
            return "${field.get(null)}"
        }

        fun sampleProperties(): Map<String, String> = withArbitrariesOutsideOfProperty {
            val info = field.getAnnotation(Info::class.java) ?: throw IllegalStateException("$field not annotated with @${Info::class.java}")
            val generator = info.generator()
            val key = field.get(null).toString()
            when {
                info.isPrefix -> Arbitraries.maps(
                    String.any().ofLength(1..10).alpha().map { key + it },
                    generator
                ).ofSize(5).fixGenSize(1).sample()
                else -> mapOf(key to generator.sample())
            }
        }

        private fun Info.generator(): Arbitrary<String> {
            var generator = generatorForType(typeHint.java).fixGenSize(1)
            generator.edgeCases { edgeCases ->
                acceptsStringConstantsIn.forEach {
                    edgeCases.add(*it.java.stringConstantFields().map { f -> f.get(null) }.toArray(::arrayOfNulls))
                }
            }
            if (commaSeparated) {
                generator = generator.list().ofSize(1..10).map { it.joinToString(",") }
            }
            return generator
        }

        private fun generatorForType(type: Class<*>): Arbitrary<String> {
            return when (type) {
                String::class.java -> String.any().ofLength(1..100)
                JdbcUrlString::class.java -> just("jdbc:h2:mem")
                ErrorOverrideString::class.java -> anyErrorOverride()
                Charset::class.java -> just(Charsets.UTF_8).map { it.name() }
                Location::class.java -> anyLocation().map { it.toString() }
                File::class.java -> String.any().alpha().ofLength(1..10).map { "target/$it" }
                Integer::class.java -> Int.any(1..Integer.MAX_VALUE).map { it.toString() }
                java.lang.Boolean::class.java -> Boolean.any().map { it.toString() }
                Version::class.java -> anyMigrationVersionString()
                ValidatePattern::class.java -> anyValidatePattern().map { it.pattern() }
                Class::class.java -> just(UniversalDummy::class.java.name)
                else -> throw IllegalStateException("Field type not implemented: $type")
            }
        }
    }

    internal class ConfigKeyFields : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return PropertyNames::class.java.stringConstantFields().map { Arguments.arguments(ConfigKeyField(it)) }
        }
    }

    companion object {
        fun Class<*>.stringConstantFields(): Stream<Field> {
            return Arrays.stream(fields)
                .filter {
                    Modifier.isStatic(it.modifiers) &&
                            Modifier.isFinal(it.modifiers) &&
                            it.type == String::class.java
                }
        }
    }
}
