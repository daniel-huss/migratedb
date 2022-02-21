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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.FileVisitResult.SKIP_SUBTREE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.inputStream

/**
 * Scans the class path for resources and classes that might be relevant for database migrations.
 */
class Scanner(val onUnprocessablePath: (Path) -> Unit = {}) {
    data class Config(
        /**
         * Class directories and jar files to include in the scan.
         */
        val classPath: List<Path>,
        /**
         * Resource name prefixes to include in the scan. To include package `com.foo.bar` you specify `com/foo/bar` here.
         * The separator is always a forward slash and does not depend on the file system implementation.
         */
        val includedPaths: List<String>,
        /**
         * In addition to [includedPaths] this further filters candidate resources. A resource only becomes part
         * of the result if this returns `true`. The resource name starts with one of [includedPaths] and
         * is always slash-separated.
         */
        val nameFilter: (resourceName: String) -> Boolean = { true },
        val followSymlinks: Boolean = false
    )

    fun scan(config: Config) = scan(listOf(config)).values.first()

    fun scan(configs: Collection<Config>): Map<Config, ScanResult> {
        return configs.asSequence().associate { it to ResultBuilder(it).build() }
    }

    private inner class ResultBuilder(private val config: Config) {
        private val foundClasses = mutableSetOf<String>()
        private val foundResources = mutableSetOf<String>()

        /**
         * Required resource name prefixes, with leading slashes removed and a single trailing slash.
         */
        private val normalizedIncludedPaths = config.includedPaths.map { it.trim('/') + "/" }
        private val fileVisitOptions = when (config.followSymlinks) {
            true -> EnumSet.of(FileVisitOption.FOLLOW_LINKS)
            false -> EnumSet.noneOf(FileVisitOption::class.java)
        }

        fun build(): ScanResult {
            config.classPath.forEach { path ->
                when {
                    Files.isDirectory(path) -> processDirectory(path)
                    path.fileName.toString().endsWith(".jar") -> processJar(path)
                    else -> onUnprocessablePath(path)
                }
            }
            return ScanResult(foundClasses, foundResources)
        }

        private fun processJar(jar: Path) {
            // Multi-release jars have additional classes under META-INF/versions/x/ -_-
            // Technically this is only relevant if the multi-release jar doesn't have a "fallback" version of
            // the versions-specific classes, but I could find no evidence that such a fallback MUST exist.
            val multiReleasePrefix = "META-INF/versions/"
            jar.inputStream().buffered().let(::ZipInputStream).use { stream ->
                generateSequence { stream.nextEntry }
                    .filterNot { it.isDirectory }
                    .forEach {
                        var slashyName = it.name.trimStart('/')
                        if (slashyName.startsWith(multiReleasePrefix)) {
                            slashyName = slashyName.substring(slashyName.indexOf('/', multiReleasePrefix.length))
                        } else if (slashyName.startsWith("META-INF/")) {
                            // other files under META-INF are not class path resources
                            return@forEach
                        }
                        process(slashyName) {
                            object : FilterInputStream(stream) {
                                override fun close() {
                                    // process() would close the stream, we don't want that
                                }
                            }
                        }
                    }
            }
        }

        private fun processDirectory(dir: Path) {
            val canonicalDir = dir.toAbsolutePath().toRealPath()
            Files.walkFileTree(
                canonicalDir,
                fileVisitOptions,
                100,
                object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        // Scan less files by skipping subtrees if possible
                        return when {
                            config.followSymlinks -> CONTINUE
                            dir.toRelativeSlashyString(canonicalDir).isParentOrChildOfIncludedPath() -> CONTINUE
                            else -> SKIP_SUBTREE
                        }
                    }

                    @Suppress("BlockingMethodInNonBlockingContext") // I don't see the non-blocking context here?!
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        process(file.toRelativeSlashyString(canonicalDir)) { Files.newInputStream(file) }
                        return CONTINUE
                    }
                })
        }

        private fun process(slashyPath: String, content: () -> InputStream) {
            if (slashyPath.isChildOfIncludedPath() && config.nameFilter(slashyPath)) {
                when {
                    slashyPath.endsWith(".class") -> content().use { processClassFile(it) }
                    else -> foundResources.add(slashyPath)
                }
            }
        }

        private fun processClassFile(byteCode: InputStream) {
            ClassReader(byteCode).accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<out String>?
                ) {
                    val isProbablyInstantiable = access.hasFlag(Opcodes.ACC_PUBLIC) &&
                            !access.hasFlag(Opcodes.ACC_ABSTRACT) &&
                            !access.hasFlag(Opcodes.ACC_INTERFACE) &&
                            !access.hasFlag(Opcodes.ACC_ANNOTATION) &&
                            !access.hasFlag(Opcodes.ACC_ENUM)
                    if (isProbablyInstantiable) {
                        foundClasses.add(name.internalNameToClassName())
                    }
                    super.visit(version, access, name, signature, superName, interfaces)
                }
            }, ClassReader.SKIP_DEBUG)
        }

        private fun Int.hasFlag(flag: Int) = and(flag) == flag

        private fun String.isChildOfIncludedPath() = normalizedIncludedPaths.any { this.startsWith(it) }
        private fun String.isParentOrChildOfIncludedPath(): Boolean {
            val asParent = when {
                isEmpty() || endsWith('/') -> this
                else -> "$this/"
            }
            return normalizedIncludedPaths.any { it.startsWith(asParent) || this.startsWith(it) }
        }

        private fun String.internalNameToClassName() = replace("/", ".")

        private fun Path.toRelativeSlashyString(parent: Path) = parent.relativize(this)
            .also { check(!it.isAbsolute) }
            .joinToString("/")
    }
}
