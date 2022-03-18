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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import migratedb.core.api.Location
import migratedb.core.api.MigrationVersion
import migratedb.core.api.configuration.ConfigKey.ErrorOverrideString
import migratedb.core.api.configuration.ConfigKey.Info
import migratedb.core.api.configuration.ConfigKey.JdbcUrlString
import migratedb.core.api.pattern.ValidatePattern
import migratedb.testing.CoreTest
import migratedb.testing.MigrateDbDomain
import migratedb.testing.MyNoOp
import migratedb.testing.anyErrorOverride
import migratedb.testing.anyLocation
import migratedb.testing.anyMigrationVersionString
import migratedb.testing.anyValidatePattern
import migratedb.testing.comparableProperties
import migratedb.testing.withArbitrariesOutsideOfProperty
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitraries.just
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.domains.Domain
import net.jqwik.kotlin.api.any
import net.jqwik.kotlin.api.ofLength
import net.jqwik.kotlin.api.ofSize
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
internal class ClassicConfigurationTest : CoreTest() {
    @Property
    fun `configure(Configuration) copies all properties except class loader into empty configuration`(
        @ForAll source: ClassicConfiguration
    ) {
        // when
        val target = ClassicConfiguration()
        target.configure(source)

        // then
        target.comparableProperties().shouldBe(source.comparableProperties())
    }

    @Property
    fun `configure(Configuration) copies all properties except class loader into non-empty configuration`(
        @ForAll source: ClassicConfiguration,
        @ForAll target: ClassicConfiguration
    ) {
        // when
        target.configure(source)

        // then
        target.comparableProperties().shouldBe(source.comparableProperties())
    }

    @ParameterizedTest
    @ArgumentsSource(ConfigKeyFields::class)
    fun `configure(Properties) supports all config keys`(field: Field) {
        // given
        val config = ClassicConfiguration()
        val oldProperties = config.comparableProperties()

        // when
        config.configure(field.sampleProperties())

        // then
        config.comparableProperties().shouldNotBe(oldProperties)
    }


    internal class ConfigKeyFields : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return ConfigKey::class.java.stringConstantFields().map { Arguments.arguments(it) }
        }
    }

    companion object {
        fun Field.sampleProperties(): Map<String, String> = withArbitrariesOutsideOfProperty {
            val info = getAnnotation(Info::class.java) ?: throw IllegalStateException("$this not annotated with @${Info::class.java}")
            val generator = info.generator()
            val key = get(null).toString()
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
                    edgeCases.add(*it.java.stringConstantFields().map { field -> field.get(null) }.toArray(::arrayOfNulls))
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
                MigrationVersion::class.java -> anyMigrationVersionString()
                ValidatePattern::class.java -> anyValidatePattern().map { it.pattern() }
                Class::class.java -> just(MyNoOp::class.java.name)
                else -> throw IllegalStateException("Field type not implemented: $type")
            }
        }

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
