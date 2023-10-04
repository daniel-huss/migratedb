/*
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.api.logging

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import migratedb.v1.core.internal.logging.*
import migratedb.v1.core.internal.util.ClassUtils.defaultClassLoader
import migratedb.v1.core.testing.LogRecorder
import org.junit.jupiter.api.Test
import java.net.URLClassLoader

internal class LogSystemsTest {

    @Test
    fun `fromStrings() supports custom class names`() {
        // given
        val logSystem = LogSystems.fromStrings(
            setOf(LogSystemA::class.java.name, LogSystemB::class.java.name, LogSystems.NONE),
            defaultClassLoader(),
            null
        )

        // when
        logA.entries.clear()
        logB.entries.clear()
        logSystem.error("Test", "Test")

        // then
        logA.entries.map { "${it.logName}:${it.message}" }
            .shouldContainExactly("Test:Test")
    }

    @Test
    fun `fromStrings() accepts empty set`() {
        LogSystems.fromStrings(emptySet(), defaultClassLoader(), null)
            .shouldBe(NoLogSystem.INSTANCE)
    }

    @Test
    fun `fromStrings() supports Apache Commons`() {
        LogSystems.fromStrings(setOf(LogSystems.APACHE_COMMONS), defaultClassLoader(), null)
            .shouldBeInstanceOf<ApacheCommonsLogSystem>()
    }

    @Test
    fun `fromStrings() supports Fallback`() {
        LogSystems.fromStrings(setOf(LogSystems.CONSOLE), defaultClassLoader(), logA)
            .shouldBeSameInstanceAs(logA)
    }

    @Test
    fun `fromStrings() supports SLF4J`() {
        LogSystems.fromStrings(setOf(LogSystems.SLF4J), defaultClassLoader(), null)
            .shouldBeInstanceOf<Slf4jLogSystem>()
    }

    @Test
    fun `fromStrings() supports Auto Detection`() {
        LogSystems.fromStrings(setOf(LogSystems.AUTO_DETECT), defaultClassLoader(), logA)
            .shouldBeInstanceOf<Slf4jLogSystem>() // Because SLF4J is on the class path
    }

    @Test
    fun `fromStrings() supports Java Util Logging`() {
        LogSystems.fromStrings(setOf(LogSystems.JAVA_UTIL), defaultClassLoader(), null)
            .shouldBeInstanceOf<JavaUtilLogSystem>()
    }

    @Test
    fun `fromStrings() supports silenced logging`() {
        LogSystems.fromStrings(setOf(LogSystems.NONE), defaultClassLoader(), null)
            .shouldBeInstanceOf<NoLogSystem>()
    }

    @Test
    fun `fromStrings() supports combinations of the string constants`() {
        LogSystems.fromStrings(
            setOf(
                LogSystems.NONE,
                LogSystems.SLF4J,
                LogSystems.JAVA_UTIL,
                LogSystems.CONSOLE,
                LogSystems.AUTO_DETECT,
                LogSystems.APACHE_COMMONS
            ),
            defaultClassLoader(),
            null
        ).shouldBeInstanceOf<MultiLogSystem>()
    }

    @Test
    fun `autoDetect() uses fallback over JUL`() {
        val fallback = LogRecorder()
        LogSystems.autoDetect(blindClassLoader, fallback).shouldBe(fallback)
    }


    @Test
    fun `autoDetect() uses SLF4 if present`() {
        val slf4jClassLoader = object : ClassLoader(blindClassLoader) {
            override fun loadClass(name: String): Class<*> {
                return when {
                    name.startsWith("org.slf4j") -> defaultClassLoader().loadClass(name)
                    else -> parent.loadClass(name)
                }
            }
        }
        LogSystems.autoDetect(slf4jClassLoader, LogRecorder()).shouldBeInstanceOf<Slf4jLogSystem>()
    }

    @Test
    fun `autoDetect() uses Apache Commons Logging if present`() {
        val slf4jClassLoader = object : ClassLoader(blindClassLoader) {
            override fun loadClass(name: String): Class<*> {
                return when {
                    name.startsWith("org.apache.commons.logging") -> defaultClassLoader().loadClass(name)
                    else -> parent.loadClass(name)
                }
            }
        }
        LogSystems.autoDetect(slf4jClassLoader, LogRecorder()).shouldBeInstanceOf<ApacheCommonsLogSystem>()
    }

    @Test
    fun `autoDetect() uses JUL as last resort`() {
        LogSystems.autoDetect(blindClassLoader, null).shouldBeInstanceOf<JavaUtilLogSystem>()
    }

    class LogSystemA : LogSystem by logA
    class LogSystemB : LogSystem by logB
    companion object {
        val logA = LogRecorder(name = "a")
        val logB = LogRecorder(name = "b")
        val blindClassLoader = URLClassLoader(emptyArray(), null)
    }
}
