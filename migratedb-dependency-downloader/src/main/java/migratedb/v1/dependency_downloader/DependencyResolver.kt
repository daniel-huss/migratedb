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
package migratedb.v1.dependency_downloader

import org.apache.maven.repository.internal.*
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.RequestTrace
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.*
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import java.io.File

/**
 * Resolves Maven artifacts from remote repository, downloads into local repository.
 */
class DependencyResolver(
    private val localRepository: File,
    remoteRepositoryUrl: String,
    remoteRepositoryId: String
) : AutoCloseable {
    private val repositorySystem = newRepositorySystem()
    private val session = newSession()
    private val remoteRepository: RemoteRepository = RemoteRepository.Builder(
        remoteRepositoryId,
        "default",
        remoteRepositoryUrl
    ).build()

    @JvmName("resolveCoordinates")
    fun resolve(coordinates: Collection<String>): List<Dependency> {
        return resolve(coordinates.map { Dependency(DefaultArtifact(it), null) })
    }

    fun resolve(dependencies: Collection<Dependency>): List<Dependency> {
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

    private fun createDependencyRequest(dependencies: Collection<Dependency>): DependencyRequest {
        val collect = CollectRequest()
        collect.requestContext = "none"
        collect.dependencies = (dependencies as? List) ?: dependencies.toList()
        collect.addRepository(remoteRepository)
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

    private fun newSession(): RepositorySystemSession {
        val session = MavenRepositorySystemUtils.newSession()
        val localRepo = LocalRepository(localRepository)
        session.localRepositoryManager = repositorySystem.newLocalRepositoryManager(session, localRepo)
        return session
    }

    private fun newRepositorySystem(): RepositorySystem {
        @Suppress("DEPRECATION") val locator = DefaultServiceLocator()
        locator.addService(ArtifactDescriptorReader::class.java, DefaultArtifactDescriptorReader::class.java)
        locator.addService(VersionResolver::class.java, DefaultVersionResolver::class.java)
        locator.addService(VersionRangeResolver::class.java, DefaultVersionRangeResolver::class.java)
        locator.addService(MetadataGeneratorFactory::class.java, SnapshotMetadataGeneratorFactory::class.java)
        locator.addService(MetadataGeneratorFactory::class.java, VersionsMetadataGeneratorFactory::class.java)
        locator.addService(ModelCacheFactory::class.java, DefaultModelCacheFactory::class.java)
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        return locator.getService(RepositorySystem::class.java)
    }

    override fun close() {
        repositorySystem.shutdown()
    }
}
