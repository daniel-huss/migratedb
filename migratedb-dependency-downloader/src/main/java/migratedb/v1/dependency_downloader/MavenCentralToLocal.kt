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

package migratedb.v1.dependency_downloader

import java.io.File
import java.net.URLClassLoader

/**
 * Fetches artifacts from Maven Central into the user's default local repository.
 */
object MavenCentralToLocal {
    val resolver = DependencyResolver(
        remoteRepositoryId = "central",
        remoteRepositoryUrl = "https://repo1.maven.org/maven2/",
        localRepository = File(System.getProperty("user.home")).resolve(".m2").resolve("repository")
    ).also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }

    fun classLoaderFor(vararg coordinates: String): ClassLoader = synchronized(resolver) {
        val dependencies = resolver.resolve(coordinates.toList())
        val urls = dependencies.map { it.artifact.file.toPath().toUri().toURL() }.toTypedArray()
        return URLClassLoader(urls, Thread.currentThread().contextClassLoader)
    }
}
