/*
 * Copyright 2021 The Code Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package migratedb.integrationtest.util.dependencies

import org.apache.commons.io.FileUtils
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.RequestTrace
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import java.io.File
import java.net.URLClassLoader

/**
 * Resolves Maven artifacts from local / Maven Central repository.
 */
object DependencyResolver {

    fun Collection<Dependency>.toClassLoader(): URLClassLoader {
        val urls = map { it.artifact.file.toPath().toUri().toURL() }.toTypedArray()
        return URLClassLoader(urls, Thread.currentThread().contextClassLoader)
    }

    fun resolve(vararg coordinates: String): List<Dependency> {
        return resolve(coordinates.map { Dependency(DefaultArtifact(it), null) })
    }

    fun resolve(dependencies: List<Dependency>): List<Dependency> {
        val result = DependencyResolutionResult()
        val depRequest = createDependencyRequest(dependencies)
        addToResult(result, repositorySystem.resolveDependencies(session, depRequest).artifactResults)
        if (result.unresolvedDependencies.isNotEmpty()) {
            throw IllegalStateException("Unresolved dependencies: ${result.unresolvedDependencies}")
        }
        if (result.resolutionErrors.isNotEmpty()) {
            throw IllegalStateException("Dependency resolution errors: ${result.resolutionErrors}")
        }
        return result.resolvedDependencies.toList()
    }

    private fun createDependencyRequest(dependencies: List<Dependency>): DependencyRequest {
        val collect = CollectRequest()
        collect.requestContext = "integration-test"
        collect.dependencies = dependencies
        collect.addRepository(mavenCentral)
        val trace = RequestTrace.newChild(null, collect)
        val dependencyRequest = DependencyRequest(collect, null)
        dependencyRequest.trace = trace
        collect.trace = RequestTrace.newChild(trace, dependencyRequest)
        dependencyRequest.root = repositorySystem.collectDependencies(session, collect).root
        return dependencyRequest
    }

    private fun addToResult(result: DependencyResolutionResult, results: Collection<ArtifactResult>) {
        for (artifactResult in results) {
            val node = artifactResult.request.dependencyNode
            if (artifactResult.isResolved) {
                result.resolvedDependencies.add(node.dependency)
            } else {
                result.resolutionErrors[node.dependency] = ArrayList(artifactResult.exceptions)
            }
        }
    }

    private val repositorySystem = newRepositorySystem()
    private val session = newSession()
    private val mavenCentral: RemoteRepository = RemoteRepository.Builder(
        "central",
        "default",
        "https://repo1.maven.org/maven2/"
    ).build()

    private fun newSession(): RepositorySystemSession {
        val session: DefaultRepositorySystemSession = MavenRepositorySystemUtils.newSession()
        val localRepo = LocalRepository(FileUtils.getUserDirectory().resolve(".m2", "repository"))
        session.localRepositoryManager = repositorySystem.newLocalRepositoryManager(session, localRepo)
        return session
    }

    private fun newRepositorySystem(): RepositorySystem {
        val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        return locator.getService(RepositorySystem::class.java)
    }

    private fun File.resolve(vararg paths: String): File {
        return paths.fold(this) { f, p -> File(f, p) }
    }
}
