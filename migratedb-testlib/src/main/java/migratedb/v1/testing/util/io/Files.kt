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
package migratedb.v1.testing.util.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Creates a temporary directory on the local file system.
 * Does not litter the system temp dir, because files are stored in the build target directory of the project.
 *
 * @return An absolute path to a temp dir that exists and is removed if the JVM terminates normally.
 */
fun newTempDir(name: String, deleteOnExit: Boolean = true): Path {
    return Files.createTempDirectory(
        buildDirectory.resolve("temp").resolve(*name.toSafeFileName()).also {
            it.createDirectories()
        },
        "it"
    ).toAbsolutePath().also {
        if (deleteOnExit) {
            deleteOnExit(it)
        }
    }
}

private val dirsToDeleteOnExit = mutableSetOf<Path>().also { dirs ->
    Runtime.getRuntime().addShutdownHook(Thread {
        synchronized(dirs) {
            dirs.forEach {
                it.toFile().deleteRecursively()
            }
        }
    })
}

/**
 * Deletes the entire directory tree on JVM exit. You'll want to be extra careful with this.
 */
private fun deleteOnExit(dir: Path) = synchronized(dirsToDeleteOnExit) {
    dirsToDeleteOnExit.add(dir)
}
