package migratedb.v1.commandline.testing

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import migratedb.v1.core.internal.info.BuildInfo
import migratedb.v1.dependency_downloader.DependencyResolver
import migratedb.v1.testing.util.io.buildDirectory
import net.java.truevfs.access.TArchiveDetector
import net.java.truevfs.access.TArchiveDetector.NULL
import net.java.truevfs.access.TConfig
import net.java.truevfs.access.TFile
import org.yaml.snakeyaml.Yaml
import java.io.File

class InstallationTemplate(val directory: File, private val dependencyResolver: DependencyResolver) {
    init {
        extractArchive()
        installH2Driver()
        redirectDriverRepoToLocalServer()
    }

    fun cloneTo(target: File) {
        val dir = TFile(target, NULL)
        dir.mkdirs()
        TConfig.current().archiveDetector = NULL
        TFile(directory, NULL).cp_rp(dir)
    }

    private fun redirectDriverRepoToLocalServer() {
        val yaml = Yaml()
        val driversFile = directory.resolve("conf").resolve("drivers.yaml")
        val newDocument = driversFile.inputStream().use {
            val document: MutableMap<String?, Any?> = yaml.load(it)
            document["repo"] = LocalArtifactRepository.baseUrl
            document
        }
        driversFile.bufferedWriter().use {
            yaml.dump(newDocument, it)
        }
    }

    private fun extractArchive() {
        val archive = TFile(
            buildDirectory.resolve("migratedb-commandline-${BuildInfo.VERSION}.tar.gz").toFile(),
            TArchiveDetector.ALL
        )
        withClue("$archive.isArchive") { archive.isArchive.shouldBeTrue() }
        TConfig.current().archiveDetector = NULL
        TFile(archive, "migratedb-${BuildInfo.VERSION}")
            .cp_rp(TFile(directory, NULL))
    }

    private fun installH2Driver() {
        val driversDir = directory.resolve("drivers")
        synchronized(this) {
            dependencyResolver.resolve(listOf("com.h2database:h2:2.2.220"))
        }.forEach {
            val file = it.artifact.file
            file.copyTo(driversDir.resolve(file.name))
        }
    }
}
