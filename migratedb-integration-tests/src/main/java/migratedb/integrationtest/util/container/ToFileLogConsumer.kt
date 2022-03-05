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

package migratedb.integrationtest.util.container

import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType.STDERR
import org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

/**
 * Places container logs in `target/log/fileName.(out|err)`.
 */
class ToFileLogConsumer(fileName: String) : Consumer<OutputFrame>, AutoCloseable {

    private companion object {
        private val invalidFileNameChars = Regex("[^ \\w_.-]")

        fun toFileName(s: String): Array<String> {
            return s.split("/", File.separator)
                .filterNot { it.isEmpty() }
                .map { it.replace(invalidFileNameChars, "_") }
                .filterNot { it == "." || it == ".." }
                .toTypedArray()
        }
    }

    private val lock = Any()
    private val basePath = Paths.get("target", "log", *toFileName(fileName))

    private val outStream = lazy(lock) {
        basePath.resolveSibling("${basePath.fileName}.out").createAndOpenStream()
    }

    private val errStream = lazy(lock) {
        basePath.resolveSibling("${basePath.fileName}.err").createAndOpenStream()
    }

    private fun Path.createAndOpenStream(): BufferedOutputStream {
        parent?.createDirectories()
        return this.outputStream().buffered()
    }


    override fun accept(t: OutputFrame) {
        when (t.type) {
            STDERR -> errStream.value.write(t.bytes)
            STDOUT -> outStream.value.write(t.bytes)
            else -> {}
        }
    }

    override fun close() = synchronized(lock) {
        outStream.takeIf { it.isInitialized() }?.value.use {
            errStream.takeIf { it.isInitialized() }?.value.use { }
        }
    }
}
