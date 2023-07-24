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

package migratedb.v1.scanner

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldNotBe
import migratedb.v1.scanner.testing.FsConfigurations
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.io.path.readLines
import kotlin.io.path.writeText

internal class PathTargetTest {
    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Rejects empty file name`(fsConfig: Configuration) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            // given
            val baseDir = fs.getPath("")

            // then
            shouldThrow<IllegalArgumentException> {
                PathTarget(baseDir, false).newWriter("").use {}
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Creates non-existent parent directories`(fsConfig: Configuration) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            // given
            val baseDir = fs.rootDirectories.first()
                .resolve("create")
                .resolve("me")
                .resolve("please")
            baseDir.shouldNotExist()

            // when
            PathTarget(baseDir, false).newWriter("file").use {
                it.write("success")
            }

            // then
            baseDir.resolve("file").let {
                it.shouldBeAFile()
                it.readLines().shouldContainInOrder("success")
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Overwrite param works`(fsConfig: Configuration) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            // given
            val baseDir = fs.rootDirectories.first()
            val existingFile = baseDir.resolve("file").also {
                it.writeText("existing")
            }

            // when
            PathTarget(baseDir, true).newWriter(existingFile.fileName.toString()).use {
                it.write("new content")
            }

            // then
            baseDir.resolve("file").let {
                it.readLines().shouldContainInOrder("new content")
            }
        }
    }


    @ParameterizedTest
    @ArgumentsSource(FsConfigurations::class)
    fun `Has nice toString()`(fsConfig: Configuration) {
        Jimfs.newFileSystem(fsConfig).use { fs ->
            PathTarget(fs.getPath("dir")).let {
                it.toString().shouldNotBe(it.defaultToString())
            }
        }
    }

    private fun Any.defaultToString() = javaClass.name + "@" + Integer.toHexString(hashCode())
}
