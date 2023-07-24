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
package migratedb.v1.scanner

import org.apiguardian.api.API
import java.io.IOException
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories

/**
 * Writes to files beneath [baseDirectory], automatically creating any missing parent directories.
 */
@API(status = API.Status.STABLE, since = "1.0")
class PathTarget(private val baseDirectory: Path, private val overwrite: Boolean = true) : Target {
    @Throws(IOException::class)
    override fun newWriter(fileName: String): Writer {
        require(fileName.isNotEmpty())
        val target = baseDirectory.resolve(fileName)
        target.parent?.createDirectories()
        var openOption = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        if (overwrite) openOption += StandardOpenOption.TRUNCATE_EXISTING
        return Files.newBufferedWriter(target, *openOption)
    }

    override fun toString() = baseDirectory.toString()
}
