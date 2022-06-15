package migratedb.integrationtest.util.base

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy

class ParallelExecutionConfig : ParallelExecutionConfigurationStrategy {
    override fun createConfiguration(configurationParameters: ConfigurationParameters): ParallelExecutionConfiguration {
        return object : ParallelExecutionConfiguration {
            override fun getParallelism() = 500
            override fun getMinimumRunnable() = 1
            override fun getCorePoolSize() = 500
            override fun getMaxPoolSize() = 1000
            override fun getKeepAliveSeconds() = 5
        }
    }
}
