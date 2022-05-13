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

package migratedb.commandline.testing

import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import migratedb.commandline.DownloadDriversCommand
import migratedb.core.internal.info.BuildInfo
import migratedb.testing.util.base.Exec
import migratedb.testing.util.dependencies.DependencyResolver
import migratedb.testing.util.io.buildDirectory
import migratedb.testing.util.io.newTempDir
import net.java.truevfs.access.TArchiveDetector.ALL
import net.java.truevfs.access.TArchiveDetector.NULL
import net.java.truevfs.access.TConfig
import net.java.truevfs.access.TFile
import org.jacoco.agent.AgentJar
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.appendBytes
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

class Dsl : AutoCloseable {
    val installationDir = newInstallationDirectory()
    val configDir = installationDir.resolve("conf")
    val defaultMigrationsDir = installationDir.resolve("sql")
    val driversInSpec
        get() = configDir.resolve("drivers.yaml").let { driversFile ->
            driversFile.inputStream().buffered().use { stream ->
                Yaml().loadAs(stream, DownloadDriversCommand.DriverDefinitions::class.java)
                    .drivers.map { it.alias!! }
            }
        }


    private val jacocoDestFile = TFile(installationDir, "jacoco.exec", NULL)
    private val executable = installationDir.resolve("migratedb")

    fun exec(vararg args: String): CliOutput {
        executable.setExecutable(true)
        val process = ProcessBuilder(executable.absolutePath, *args)
            .directory(installationDir)
            .apply {
                environment()["JAVA_ARGS"] = "-javaagent:${jacocoAgentJar.absolutePath}=" +
                        "destfile=${jacocoDestFile.absolutePath}"
            }.start()
        try {
            val stdErr = Exec.async { InputStreamReader(process.errorStream, Charsets.UTF_8).use { it.readLines() } }
            val stdOut = Exec.async { InputStreamReader(process.inputStream, Charsets.UTF_8).use { it.readLines() } }
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                fail("$process seems to hang")
            }
            val exitCode = process.exitValue()
            return CliOutput(exitCode = exitCode, stdOut = stdOut(), stdErr = stdErr())
        } finally {
            process.destroyForcibly().waitFor()
            if (jacocoDestFile.exists()) {
                mergeJacocoExecutionData(jacocoDestFile)
            }
        }
    }

    override fun close() {
        installationDir.rm_r()
    }

    companion object {
        private val counter = AtomicLong()
        private val installationsBase = newTempDir("cli-installation", deleteOnExit = true)
        private val templateDir = TFile(
            installationsBase.resolve("template").toFile(),
            NULL
        ).also { template ->
            val archive = TFile(
                buildDirectory.resolve("migratedb-commandline-${BuildInfo.VERSION}.tar.gz").toFile(),
                ALL
            )
            extractArchive(archive, template)
            installH2Driver(template)
            redirectDriverRepoToLocalServer(template)
        }

        private fun redirectDriverRepoToLocalServer(template: File) {
            val yaml = Yaml()
            val driversFile = template.resolve("conf").resolve("drivers.yaml")
            val newDocument = driversFile.inputStream().use {
                val document: MutableMap<String?, Any?> = yaml.load(it)
                document["repo"] = LocalArtifactRepository.baseUrl
                document
            }
            driversFile.bufferedWriter().use {
                yaml.dump(newDocument, it)
            }
        }

        private fun extractArchive(archive: TFile, template: TFile) {
            withClue("$archive.isArchive") { archive.isArchive.shouldBeTrue() }
            TConfig.open().use { cfg ->
                cfg.archiveDetector = NULL
                TFile(archive, "migratedb-${BuildInfo.VERSION}").cp_rp(template)
            }
        }

        private fun installH2Driver(template: TFile) {
            val driversDir = template.resolve("drivers")
            DependencyResolver.resolve("com.h2database:h2:2.1.210")
                .forEach {
                    val file = it.artifact.file
                    file.copyTo(driversDir.resolve(file.name))
                }
        }

        private val jacocoAgentJar = TFile(installationsBase.toFile(), "jacoco.jar", NULL).also {
            AgentJar.extractTo(it)
        }

        /**
         * The execution data for this build.
         */
        private val myJacocoDestFile = buildDirectory.resolve("jacoco-it.exec")

        private fun mergeJacocoExecutionData(other: File) = synchronized(Dsl::class) {
            if (!myJacocoDestFile.exists()) {
                myJacocoDestFile.parent?.createDirectories()
                myJacocoDestFile.createFile()
            }
            myJacocoDestFile.appendBytes(other.readBytes())
        }

        private fun newInstallationDirectory(): TFile {
            val dir = TFile(installationsBase.resolve(counter.incrementAndGet().toString()).toFile(), NULL)
            dir.mkdirs()
            TConfig.open().use { cfg ->
                cfg.archiveDetector = NULL
                templateDir.cp_rp(dir)
            }
            return dir
        }
    }
}
