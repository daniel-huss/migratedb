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
package migratedb.v1.core.api

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import migratedb.v1.core.api.Location.ClassPathLocation
import migratedb.v1.core.api.Location.ClassPathLocation.CLASS_LIST_RESOURCE_NAME
import migratedb.v1.core.api.Location.ClassPathLocation.RESOURCE_LIST_RESOURCE_NAME
import migratedb.v1.core.internal.util.ClassUtils.defaultClassLoader
import org.junit.jupiter.api.Test
import java.net.URLClassLoader
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.reflect.KClass

internal class ClassPathLocationTest {

    class ClassA
    class ClassB
    class ClassC

    @Test
    fun `Works with multiple class index files in different resource roots`(): Unit = TestSpec(
        classIndexLocations = mapOf(
            "a/foo/bar" to listOf(ClassA::class),
            "b/foo/bar" to listOf(ClassB::class),
            "c/foo/bar" to listOf(ClassC::class)
        )
    ).use {
        ClassPathLocation("foo/bar", it.classLoader("a", "b", "c"))
            .classProvider().classes.asClue { actual ->
                actual.shouldContainExactlyInAnyOrder(ClassA::class.java, ClassB::class.java, ClassC::class.java)
            }
    }

    @Test
    fun `Works with multiple resource index files in different resource roots`(): Unit = TestSpec(
        resourceIndexLocations = mapOf(
            "a/foo/bar" to listOf("script1.sql"),
            "b/foo/bar" to listOf("script2.sql"),
            "c/foo/bar" to listOf("script3.sql")
        )
    ).use {
        ClassPathLocation("foo/bar", it.classLoader("a", "b", "c"))
            .resourceProvider().let { resources ->
                resources.getResource("script1.sql").shouldNotBeNull()
                resources.getResource("script2.sql").shouldNotBeNull()
                resources.getResource("script3.sql").shouldNotBeNull()
            }
    }


    private class TestSpec(
        classIndexLocations: Map<String, List<KClass<*>>> = emptyMap(),
        resourceIndexLocations: Map<String, List<String>> = emptyMap()
    ) : AutoCloseable {
        private val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())

        private val root = fs.rootDirectories.first()

        fun classLoader(vararg resourceRoots: String): ClassLoader {
            val urls = resourceRoots.map { root.resolve(it).toUri().toURL() }.toTypedArray()
            return URLClassLoader(urls, defaultClassLoader())
        }

        init {
            classIndexLocations.forEach { (dir, classes) ->
                root.resolve(dir).resolve(CLASS_LIST_RESOURCE_NAME)
                    .forceText(classes.joinToString("\n") { it.java.name })
            }
            resourceIndexLocations.forEach { (dir, resourceNames) ->
                root.resolve(dir).resolve(RESOURCE_LIST_RESOURCE_NAME)
                    .forceText(resourceNames.joinToString("\n"))
            }
        }

        private fun Path.forceText(text: String): Path {
            parent?.createDirectories()
            writeText(text)
            return this
        }

        override fun close() = fs.use { }
    }
}
