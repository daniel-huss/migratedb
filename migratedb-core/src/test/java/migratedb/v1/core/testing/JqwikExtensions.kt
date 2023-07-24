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

package migratedb.v1.core.testing

import migratedb.v1.core.api.Location
import migratedb.v1.core.api.Location.*
import migratedb.v1.core.api.MigrationPattern
import migratedb.v1.core.api.TargetVersion
import migratedb.v1.core.api.Version
import migratedb.v1.core.api.logging.LogSystem
import migratedb.v1.core.api.logging.LogSystems
import migratedb.v1.core.api.pattern.ValidatePattern
import migratedb.v1.core.api.pattern.ValidatePattern.validMigrationStates
import migratedb.v1.core.api.pattern.ValidatePattern.validMigrationTypes
import migratedb.v1.core.internal.util.ClassUtils
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.arbitraries.ArrayArbitrary
import net.jqwik.kotlin.api.any
import java.nio.file.Paths
import kotlin.reflect.KClass

fun <T> withArbitrariesOutsideOfProperty(action: () -> T) = synchronized(Arbitraries::class.java, action)

fun <T : Any> Arbitrary<T>.array(elementType: KClass<in T>): ArrayArbitrary<T, Array<T>> {
    val arrayType = java.lang.reflect.Array.newInstance(elementType.java, 0).javaClass
    @Suppress("UNCHECKED_CAST")
    return array(arrayType) as ArrayArbitrary<T, Array<T>>
}

fun anyValidatePattern(): Arbitrary<ValidatePattern> {
    return Combinators.combine(
        Arbitraries.of(validMigrationTypes),
        Arbitraries.of(validMigrationStates)
    ).`as` { type, state ->
        ValidatePattern.fromPattern("$type:$state")
    }
}

fun anyLogSystemAsString(): Arbitrary<String> {
    return Arbitraries.of(
        LogSystems.APACHE_COMMONS,
        LogSystems.AUTO_DETECT,
        LogSystems.CONSOLE,
        LogSystems.JAVA_UTIL,
        LogSystems.NONE,
        LogSystems.SLF4J,
        UniversalDummy::class.java.name
    )
}

fun anyLogSystem(): Arbitrary<LogSystem> = anyLogSystemAsString().map {
    LogSystems.fromStrings(setOf(it), ClassUtils.defaultClassLoader(), null)
}

fun anyLocation(): Arbitrary<Location> {
    val directoryNames = String.any().alpha().list().ofMinSize(1).ofMaxSize(10)
    val classPathLocations = directoryNames.map {
        ClassPathLocation(it.joinToString("/"), ClassUtils.defaultClassLoader())
    }
    val fileSystemLocations = directoryNames.map {
        FileSystemLocation(Paths.get("target", *it.toTypedArray()))
    }
    val customLocations = Arbitraries.just(UniversalDummy().let {
        CustomLocation(it, it)
    })

    return Arbitraries.oneOf(classPathLocations, fileSystemLocations, customLocations)
}

fun anySchemaObjectName(): Arbitrary<String> = String.any()
    .ofMinLength(1)
    .ofMaxLength(16)
    .alpha()
    .withChars('_')

fun anyMigrationPattern(): Arbitrary<MigrationPattern> = String.any()
    .alpha()
    .numeric()
    .withChars('_', '.')
    .ofMinLength(1)
    .ofMaxLength(20)
    .map(::MigrationPattern)

fun anyMigrationVersionString(): Arbitrary<String> = String.any()
    .numeric()
    .ofMinLength(1)
    .ofMaxLength(10)
    .list()
    .ofMinSize(1)
    .ofMaxSize(10)
    .map {
        it.joinToString(".")
    }

fun anyMigrationVersion(): Arbitrary<Version> = anyMigrationVersionString().map(Version::parse)

fun anyTargetVersionString(): Arbitrary<String> = anyMigrationVersionString()
    .edgeCases {
        it.add("next", "latest", "current")
    }

fun anyTargetVersion(): Arbitrary<TargetVersion> = anyTargetVersionString().map(TargetVersion::parse)
