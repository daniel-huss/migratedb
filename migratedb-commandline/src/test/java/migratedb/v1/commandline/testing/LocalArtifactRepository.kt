/*
 * Copyright 2022-2023 The MigrateDB contributors
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

package migratedb.v1.commandline.testing

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import migratedb.v1.dependency_downloader.MavenCentralToLocal
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.*


/**
 * Minimal implementation of m2 repo that delegates to DependencyResolver, just enough to make driver download work
 * without hammering Maven Central.
 */
object LocalArtifactRepository {
    private val binding = InetSocketAddress(InetAddress.getLoopbackAddress(), 0)
    private val dependencyResolver = MavenCentralToLocal.resolver

    private val server = Server(binding).also { srv ->
        srv.handler = object : AbstractHandler() {
            override fun handle(
                target: String,
                baseRequest: Request,
                request: HttpServletRequest,
                response: HttpServletResponse
            ) {
                val coordinates = toCoordinates(baseRequest.httpURI.decodedPath)
                val file = dependencyResolver.resolve(listOf(coordinates))
                    .single { "${it.artifact.groupId}:${it.artifact.artifactId}:${it.artifact.version}" == coordinates }
                    .artifact.file
                response.status = 200
                response.contentType = "application/octet-stream"
                response.outputStream.use {
                    Files.copy(file.toPath(), it)
                }
            }
        }
        srv.start()
    }

    val baseUrl get() = server.uri.toString()

    private fun toCoordinates(urlPath: String): String {
        val segments = LinkedList(urlPath.trim('/').split('/'))
        segments.removeLast() // file name
        val version = segments.removeLast()
        val artifactId = segments.removeLast()
        val groupId = segments.joinToString(".")
        return "$groupId:$artifactId:$version"
    }
}
