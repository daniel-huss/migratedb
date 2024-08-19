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

package migratedb.v1.commandline.testing

import java.io.File

/**
 * Represents the JAR file that is generated during the build and contains the migrations in
 * `migratedb.v1.commandline.testing.migration` (+ a class index, so they are found when using the classpath: location).
 */
object TestMigrationsJar {
    private const val resourceName = "/test-resources/test-migrations-jar.jar" // see generation in pom.xml

    fun copyTo(file: File) {
        file.parentFile.mkdirs()
        TestMigrationsJar::class.java.getResourceAsStream(resourceName)!!.use { inStream ->
            file.outputStream().use { outStream ->
                inStream.transferTo(outStream)
            }
        }
    }
}
