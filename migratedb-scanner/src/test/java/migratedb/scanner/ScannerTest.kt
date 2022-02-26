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
import migratedb.scanner.ScannerTest.Kind.ANNOTATION
import migratedb.scanner.ScannerTest.Kind.ENUM
import migratedb.scanner.ScannerTest.Kind.INTERFACE
import migratedb.scanner.ScannerTest.Kind.PLAIN_CLASS
import migratedb.scanner.ScannerTest.Mod.ABSTRACT
import migratedb.scanner.ScannerTest.Mod.FINAL
import migratedb.scanner.ScannerTest.Mod.PRIVATE
import migratedb.scanner.ScannerTest.Mod.PROTECTED
import migratedb.scanner.ScannerTest.Mod.PUBLIC
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_ANNOTATION
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists
import kotlin.io.path.outputStream

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


    // Boring test DSL implementation below this line

    private fun withDsl(fsConfig: Configuration, block: (Dsl).() -> Unit) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            Dsl(fs).block()
        }
    }

    private enum class Mod(val opcode: Int) {
        ABSTRACT(ACC_ABSTRACT), PUBLIC(ACC_PUBLIC), PRIVATE(ACC_PRIVATE), PROTECTED(ACC_PROTECTED), FINAL(ACC_FINAL)
    }

    private enum class Kind(val opcode: Int) {
        PLAIN_CLASS(0), ENUM(ACC_ENUM), ANNOTATION(ACC_ANNOTATION), INTERFACE(ACC_INTERFACE);
    }

    private class Dsl(val fs: FileSystem) {
        fun jar(path: Path, block: (JarBuilder).() -> Unit): Path {
            return path.also {
                JarBuilder(it).use(block)
            }
        }

        fun clazz(dir: Path, binaryName: String, kind: Kind, vararg mods: Mod): Path {
            return dir.plus("$binaryName.class").createFileAndParents().also { path ->
                path.outputStream().use {
                    writeClass(it, binaryName, kind, mods)
                }
            }
        }

        fun resource(path: Path): Path {
            return path.createFileAndParents()
        }

        inner class JarBuilder(path: Path) : AutoCloseable {
            private val stream = ZipOutputStream(path.createFileAndParents().outputStream()).also {
                // The first entry must be META-INF/MANIFEST.MF
                try {
                    it.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                } catch (e: Exception) {
                    it.use { throw e }
                }
            }

            fun resource(path: String) {
                stream.putNextEntry(ZipEntry(path.trimStart('/')))
            }

            fun clazz(binaryName: String, kind: Kind, vararg mods: Mod) {
                stream.putNextEntry(ZipEntry("$binaryName.class"))
                writeClass(stream, binaryName, kind, mods)
            }

            fun multiReleaseClazz(binaryName: String, version: Int, kind: Kind, vararg mods: Mod) {
                stream.putNextEntry(ZipEntry("META-INF/versions/$version/$binaryName.class"))
                writeClass(stream, binaryName, kind, mods)
            }

            override fun close() = stream.close()
        }

        private fun writeClass(stream: OutputStream, binaryName: String, kind: Kind, mods: Array<out Mod>) {
            val w = ClassWriter(0)
            val access = mods.fold(kind.opcode) { opcode, mod -> opcode.or(mod.opcode) }
            val superName = when (kind) {
                Kind.ENUM -> "java/lang/Enum"
                else -> "java/lang/Object"
            }
            w.visit(Opcodes.V17, access, binaryName, null, superName, null)
            stream.write(w.toByteArray())
        }

        private fun Path.createFileAndParents() = this.also { if (!exists()) parent?.createDirectories().also { createFile() } }
        fun String.toPath() = fs.rootDirectories.first().plus(this)
        operator fun Path.plus(relativePath: String) = relativePath.split('/').fold(this) { p, next -> p.resolve(next) }
    }
}
