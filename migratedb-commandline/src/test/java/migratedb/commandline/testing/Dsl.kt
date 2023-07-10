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
import migratedb.commandline.DownloadDriversCommand
import migratedb.dependency_downloader.MavenCentralToLocal
import migratedb.testing.util.base.Exec
import migratedb.testing.util.io.buildDirectory
import migratedb.testing.util.io.newTempDir
import net.java.truevfs.access.TArchiveDetector.NULL
import net.java.truevfs.access.TFile
import org.jacoco.agent.AgentJar
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.appendBytes
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

class Dsl : AutoCloseable {
    val installationDir = installationsBase.resolve(counter.incrementAndGet().toString()).also {
        installationTemplate.cloneTo(it)
    }
    val configDir = installationDir.resolve("conf")
    val defaultMigrationsDir = installationDir.resolve("sql")
    val defaultJarsDir = installationDir.resolve("jars")

    val driversInSpec
        get() = configDir.resolve("drivers.yaml").let { driversFile ->
            driversFile.inputStream().buffered().use { stream ->
                Yaml().loadAs(stream, DownloadDriversCommand.DriverDefinitions::class.java)
                    .drivers.map { it.alias!! }
            }
        }

    private val jacocoDestFile = TFile(installationDir, "jacoco.exec", NULL)
    private val executable = installationDir.resolve("migratedb")

    fun exec(vararg args: String, stdin: (OutputStream) -> Unit = {}) = exec(args.toList(), stdin)
    fun exec(args: List<String>, stdin: (OutputStream) -> Unit = {}): CliOutput {
        executable.setExecutable(true)
        val process = ProcessBuilder(listOf(executable.absolutePath) + args)
            .directory(installationDir)
            .apply {
                environment()["JAVA_ARGS"] = "-javaagent:${jacocoAgentJar.absolutePath}=" +
                        "destfile=${jacocoDestFile.absolutePath}"
            }.start()
        try {
            Exec.async {
                stdin(process.outputStream)
                process.outputStream.flush()
                process.outputStream.close()
            }
            val stdErr = Exec.async { InputStreamReader(process.errorStream, Charsets.UTF_8).use { it.readLines() } }
            val stdOut = Exec.async { InputStreamReader(process.inputStream, Charsets.UTF_8).use { it.readLines() } }
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                fail("$process seems to be frozen")
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
        TFile(installationDir, NULL).rm_r()
    }

    companion object {
        private val counter = AtomicLong()
        private val installationsBase = newTempDir("cli-installation", deleteOnExit = true).toFile()
        private val installationTemplate = InstallationTemplate(
            directory = installationsBase.resolve("template"),
            dependencyResolver = MavenCentralToLocal.resolver
        )

        /**
         * The execution data for this build.
         */
        private val myJacocoDestFile = buildDirectory.resolve("jacoco-it.exec")

        private val jacocoAgentJar = File(installationsBase, "jacoco.jar").also {
            AgentJar.extractTo(it)
        }

        private fun mergeJacocoExecutionData(other: File) = synchronized(Dsl::class) {
            if (!myJacocoDestFile.exists()) {
                myJacocoDestFile.parent?.createDirectories()
                myJacocoDestFile.createFile()
            }
            myJacocoDestFile.appendBytes(other.readBytes())
        }
    }
}
