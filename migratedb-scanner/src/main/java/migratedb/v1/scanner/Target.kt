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
package migratedb.v1.scanner

import org.apiguardian.api.API
import java.io.IOException
import java.io.Writer

@API(status = API.Status.STABLE, since = "1.0")
interface Target {
    /**
     * @param fileName A non-empty file name.
     * @throws IllegalArgumentException If [fileName] is empty.
     * @throws IOException If some file system error occurs, e.g., if the file cannot be opened for writing.
     */
    @Throws(IOException::class)
    fun newWriter(fileName: String): Writer
}
