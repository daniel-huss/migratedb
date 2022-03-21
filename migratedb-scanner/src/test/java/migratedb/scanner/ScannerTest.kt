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

package migratedb.scanner

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import migratedb.core.api.Location.ClassPathLocation
import migratedb.scanner.testing.Dsl
import migratedb.scanner.testing.FsConfigurations
import migratedb.scanner.testing.Kind.ANNOTATION
import migratedb.scanner.testing.Kind.ENUM
import migratedb.scanner.testing.Kind.INTERFACE
import migratedb.scanner.testing.Kind.PLAIN_CLASS
import migratedb.scanner.testing.Mod.ABSTRACT
import migratedb.scanner.testing.Mod.FINAL
import migratedb.scanner.testing.Mod.PRIVATE
import migratedb.scanner.testing.Mod.PROTECTED
import migratedb.scanner.testing.Mod.PUBLIC
import migratedb.testing.util.io.resolve
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo

internal class ScannerTest {

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Empty result on empty class path`(fsConfig: Configuration) = withDsl(fsConfig) {
        val result = Scanner().scan(Scanner.Config(emptySet(), emptySet()))
        result.foundClasses.shouldBeEmpty()
        result.foundResources.shouldBeEmpty()
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Empty result if no path is included`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val existingResource = resource("not/included/file.sql".toPath())

        // when
        val result = Scanner().scan(
            Scanner.Config(
                includedPaths = setOf("included"), scope = setOf(existingResource.parent)
            )
        )

        // then
        result.foundClasses.shouldBeEmpty()
        result.foundResources.shouldBeEmpty()
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Supports multi-release jar`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val jarFile = jar("jars/someJar.jar".toPath()) {
            multiReleaseClazz("foo/bar/Clazz\$Nested", 17, PLAIN_CLASS, PUBLIC, FINAL)
        }

        // when
        val result = Scanner().scan(Scanner.Config(setOf(jarFile), setOf("foo")))

        // then
        result.foundClasses.shouldContainExactly("foo.bar.Clazz\$Nested")
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Ignores everything that is not below an included path`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val classpathDir = "someClasspathDir".toPath()
        val jarFile = jar("jars/someJar.jar".toPath()) {
            clazz("foo/included/Clazz1", PLAIN_CLASS, PUBLIC)
            clazz("foo/excluded/Clazz2", PLAIN_CLASS, PUBLIC)
            clazz("bar/Clazz3", PLAIN_CLASS, PUBLIC)
        }
        clazz(classpathDir, "foo/included/Clazz4", PLAIN_CLASS, PUBLIC)
        clazz(classpathDir, "foo/excluded/Clazz5", PLAIN_CLASS, PUBLIC)
        resource(classpathDir.plus("foo/included/r1.sql"))
        resource(classpathDir.plus("foo/excluded/r2.sql"))
        resource(classpathDir.plus("bar/r3.sql"))

        // when
        val result = Scanner().scan(Scanner.Config(setOf(jarFile, classpathDir), setOf("foo/included")))

        // then
        result.foundClasses.shouldContainExactlyInAnyOrder("foo.included.Clazz1", "foo.included.Clazz4")
        result.foundResources.shouldContainExactlyInAnyOrder("foo/included/r1.sql")
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Name filter works`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val classpathDir = "someClasspathDir".toPath()
        val jarFile = jar("jars/someJar.jar".toPath()) {
            clazz("foo/yes/Clazz1", PLAIN_CLASS, PUBLIC)
            clazz("foo/nope/Class2", PLAIN_CLASS, PUBLIC)
            resource("foo/yes/r1.sql")
            resource("foo/nope/r2.sql")
        }
        clazz(classpathDir, "foo/yes/Clazz3", PLAIN_CLASS, PUBLIC)
        clazz(classpathDir, "foo/nope/Clazz4", PLAIN_CLASS, PUBLIC)
        resource(classpathDir.plus("foo/yes/r3.sql"))
        resource(classpathDir.plus("foo/nope/r4.sql"))

        // when
        val result = Scanner().scan(Scanner.Config(setOf(jarFile, classpathDir), setOf("foo"), { !it.contains("nope") }))

        // then
        result.foundClasses.shouldContainExactlyInAnyOrder("foo.yes.Clazz1", "foo.yes.Clazz3")
        result.foundResources.shouldContainExactlyInAnyOrder("foo/yes/r1.sql", "foo/yes/r3.sql")
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Callback onUnprocessablePath works`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val unprocessablePath = resource("warfile.war".toPath())
        val processablePath = jar("jarfile.jar".toPath()) {}
        val callbackCount = AtomicLong(0)

        // when
        Scanner { callbackCount.incrementAndGet() }
            .scan(Scanner.Config(setOf(processablePath, unprocessablePath), setOf("foo")))

        // then
        callbackCount.get().shouldBe(1)
    }


    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Non-instantiable classes are ignored`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val classpathDir = "classpathDir".toPath()
        clazz(classpathDir, "foo/PrivateClass", PLAIN_CLASS, PRIVATE)
        clazz(classpathDir, "foo/ProtectedClass", PLAIN_CLASS, PROTECTED)
        clazz(classpathDir, "foo/AbstractClass", PLAIN_CLASS, PUBLIC, ABSTRACT)
        clazz(classpathDir, "foo/EnumClass", ENUM, PUBLIC)
        clazz(classpathDir, "foo/InterfaceClass", INTERFACE, PUBLIC)
        clazz(classpathDir, "foo/AnnotationClass", ANNOTATION, PUBLIC)
        clazz(classpathDir, "foo/InstantiableClass", PLAIN_CLASS, PUBLIC)
        val jarFile = jar("someJar.jar".toPath()) {
            clazz("bar/PrivateClass", PLAIN_CLASS, PRIVATE)
            clazz("bar/ProtectedClass", PLAIN_CLASS, PROTECTED)
            clazz("bar/AbstractClass", PLAIN_CLASS, PUBLIC, ABSTRACT)
            clazz("bar/EnumClass", ENUM, PUBLIC)
            clazz("bar/InterfaceClass", INTERFACE, PUBLIC)
            clazz("bar/AnnotationClass", ANNOTATION, PUBLIC)
            clazz("bar/InstantiableClass", PLAIN_CLASS, PUBLIC)
        }

        // when
        val result = Scanner().scan(Scanner.Config(setOf(classpathDir, jarFile), setOf("foo", "bar")))

        // then
        result.foundClasses.shouldContainExactlyInAnyOrder("foo.InstantiableClass", "bar.InstantiableClass")
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Follows symlinks if configured to do so`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val linkTarget = "parent/linkTarget".toPath().also { it.createDirectories() }
        val classpathDir = "parent/classpathDir".toPath()
        classpathDir.resolve("symlink").also {
            it.parent.createDirectories()
            it.createSymbolicLinkPointingTo(linkTarget)
        }
        resource(linkTarget.resolve("foo").resolve("resource.sql"))

        // when
        val result = Scanner().scan(Scanner.Config(setOf(classpathDir), setOf("symlink/foo"), followSymlinks = true))

        // then
        result.foundResources.shouldContainExactlyInAnyOrder("symlink/foo/resource.sql")
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Does not follow symlinks if configured not to`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val linkTarget = "parent/linkTarget".toPath().also { it.createDirectories() }
        val classpathDir = "parent/classpathDir".toPath()
        classpathDir.resolve("symlink").also {
            it.parent.createDirectories()
            it.createSymbolicLinkPointingTo(linkTarget)
        }
        resource(linkTarget.resolve("foo").resolve("resource.sql"))

        // when
        val result = Scanner().scan(Scanner.Config(setOf(classpathDir), setOf("symlink/foo"), followSymlinks = false))

        // then
        result.foundResources.shouldBeEmpty()
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Works with ClassPathLocation`(fsConfig: Configuration) = withDsl(fsConfig) {
        // given
        val classpathDir = "classpathDir".toPath()
        clazz(classpathDir, "foo/InstantiableClass", PLAIN_CLASS, PUBLIC)
        resource(classpathDir.resolve("foo/script1.sql"))
        resource(classpathDir.resolve("bar/script2.sql"))
        resource(classpathDir.resolve("bar/baz/script3.sql"))
        val jarFile = jar("someJar.jar".toPath()) {
            clazz("bar/InstantiableClass", PLAIN_CLASS, PUBLIC)
        }

        // when
        Scanner().scan(Scanner.Config(setOf(classpathDir, jarFile), setOf("foo", "bar"))).also {
            it.writeTo(PathTarget(classpathDir.resolve("db", "migration")))
        }
        val urls = listOf(classpathDir, jarFile).map { it.toUri().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, null)
        val actual = ClassPathLocation("db/migration", classLoader)

        // then
        actual.classProvider().classes.map { it.name }
            .shouldContainExactlyInAnyOrder("foo.InstantiableClass", "bar.InstantiableClass")
        actual.resourceProvider().getResources("", ".sql").map { it.name }
            .shouldContainExactlyInAnyOrder("foo/script1.sql", "bar/script2.sql", "bar/baz/script3.sql")
    }

    private fun withDsl(fsConfig: Configuration, block: (Dsl).() -> Unit) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            Dsl(fs).block()
        }
    }
}
