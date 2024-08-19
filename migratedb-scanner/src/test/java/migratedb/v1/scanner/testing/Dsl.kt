/*
 * Copyright 2022-2024 The MigrateDB contributors
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

package migratedb.v1.scanner.testing

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.outputStream

class Dsl(private val fs: FileSystem) {
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
        w.visit(Opcodes.V11, access, binaryName, null, superName, null)
        stream.write(w.toByteArray())
    }

    private fun Path.createFileAndParents() = this.also { if (!exists()) parent?.createDirectories().also { createFile() } }
    fun String.toPath() = fs.rootDirectories.first().plus(this)
    operator fun Path.plus(relativePath: String) = relativePath.split('/').fold(this) { p, next -> p.resolve(next) }
}
