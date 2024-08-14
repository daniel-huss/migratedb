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
