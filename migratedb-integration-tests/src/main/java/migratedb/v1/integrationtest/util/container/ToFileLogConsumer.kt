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

package migratedb.v1.integrationtest.util.container

import migratedb.v1.testing.util.io.toSafeFileName
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType.STDERR
import org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

/**
 * Places container logs in `target/container-logs/fileName.txt`.
 */
class ToFileLogConsumer constructor(fileName: String) : Consumer<OutputFrame>, AutoCloseable {
    private val lock = object : Any() {}
    private val pathWithoutExtension = Paths.get("target", "container-logs", *fileName.toSafeFileName())

    private val stream = lazy(lock) {
        val pathWithExtension = pathWithoutExtension.resolveSibling("${pathWithoutExtension.fileName}.txt")
        pathWithExtension.parent?.createDirectories()
        pathWithExtension.outputStream()
    }

    override fun accept(t: OutputFrame) {
        when (t.type) {
            STDERR, STDOUT -> stream.value.write(t.bytes)
            else -> {}
        }
    }

    override fun close() = synchronized(lock) {
        stream.takeIf { it.isInitialized() }?.value.use {}
    }
}
