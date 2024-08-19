/*
 * Copyright 2021-2024 The MigrateDB contributors.
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
package migratedb.v1.dependency_downloader

import org.eclipse.aether.graph.Dependency

data class DependencyResolutionResult(
    /**
     * The dependencies that were successfully resolved.
     */
    val resolvedDependencies: MutableList<Dependency> = ArrayList(),

    /**
     * The dependencies that could not be resolved.
     */
    val unresolvedDependencies: MutableList<Dependency> = ArrayList(),

    /**
     * The errors that prevented dependencies from being resolved.
     */
    val resolutionErrors: MutableMap<Dependency, List<Exception>> = HashMap()
)
