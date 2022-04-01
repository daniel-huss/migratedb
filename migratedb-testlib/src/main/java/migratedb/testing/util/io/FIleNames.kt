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

package migratedb.testing.util.io

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val invalidFileNameChars = Regex("[^ \\w_.-]")

fun String.toSafeFileName(): Array<String> {
    return this.split("/", File.separator)
        .filterNot { it.isBlank() }
        .map { it.replace(invalidFileNameChars, "_") }
        .filterNot { it == "." || it == ".." }
        .toTypedArray()
}

/**
 * @return The value of ${project.build.directory}
 */
val buildDirectory: Path
    get() = Paths.get("target").toAbsolutePath()

fun Path.resolve(vararg paths: String) = paths.fold(this) { p, seg -> p.resolve(seg) }
