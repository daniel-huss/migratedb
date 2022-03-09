package migratedb.integrationtest.util.base

import migratedb.integrationtest.util.base.FileNames.toSafeFileName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

/**
 * Creates a temporary directory for database systems that work on the local file system.
 * Does not litter the system temp dir, because files are stored in the build target directory of the project.
 *
 * @return An absolute path to a temp dir that exists and is removed if the JVM terminates normally.
 */
fun createDatabaseTempDir(name: String, deleteOnExit: Boolean = true): Path {
    return Files.createTempDirectory(
        Paths.get("target", "database-temp", *name.toSafeFileName()).also {
            it.createDirectories()
        },
        "it"
    ).toAbsolutePath().also {
        if (deleteOnExit) {
            Cleanup.deleteOnExit(it)
        }
    }
}

object FileNames {
    private val invalidFileNameChars = Regex("[^ \\w_.-]")

    fun String.toSafeFileName(): Array<String> {
        return this.split("/", File.separator)
            .filterNot { it.isBlank() }
            .map { it.replace(invalidFileNameChars, "_") }
            .filterNot { it == "." || it == ".." }
            .toTypedArray()
    }
}

private object Cleanup {
    private val dirs = mutableSetOf<Path>()

    fun deleteOnExit(dir: Path) = synchronized(dirs) {
        dirs.add(dir)
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            synchronized(dirs) {
                dirs.forEach {
                    it.toFile().deleteRecursively()
                }
            }
        })
    }
}
