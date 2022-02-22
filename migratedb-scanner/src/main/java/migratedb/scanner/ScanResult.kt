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
package migratedb.scanner

import migratedb.core.api.Location.ClassPathLocation
import java.io.IOException
import java.io.Writer
import java.util.Set.copyOf

class ScanResult internal constructor(foundClasses: Collection<String>, foundResources: Collection<String>) {
    val foundClasses: Set<String> = copyOf(foundClasses)
    val foundResources: Set<String> = copyOf(foundResources)

    @Throws(IOException::class)
    fun writeTo(target: Target) {
        target.newWriter(ClassPathLocation.CLASS_LIST_RESOURCE_NAME).use { classesWriter ->
            target.newWriter(ClassPathLocation.RESOURCE_LIST_RESOURCE_NAME).use { resourcesWriter ->
                writeList(classesWriter, foundClasses)
                writeList(resourcesWriter, foundResources)
            }
        }
    }

    companion object {
        private fun writeList(w: Writer, lines: Collection<String>) {
            for (line in lines) {
                w.write(line)
                w.write("\n")
            }
        }
    }
}
