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

package migratedb.v1.integrationtest.util.base

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy

class ParallelExecutionConfig : ParallelExecutionConfigurationStrategy {
    override fun createConfiguration(configurationParameters: ConfigurationParameters): ParallelExecutionConfiguration {
        val approximateTotalNumberOfTests = 1000

        return object : ParallelExecutionConfiguration {
            // We need about as many threads as there are tests to make good use of the container pool.
            // This is going to be much more lightwight once Loom delivers virtual threads.
            override fun getParallelism() = approximateTotalNumberOfTests
            override fun getMinimumRunnable() = 1
            override fun getCorePoolSize() = 1
            override fun getMaxPoolSize() = approximateTotalNumberOfTests
            override fun getKeepAliveSeconds() = 5
        }
    }
}
