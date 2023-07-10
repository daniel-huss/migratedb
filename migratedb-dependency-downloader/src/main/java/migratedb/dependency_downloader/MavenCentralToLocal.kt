package migratedb.dependency_downloader

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
