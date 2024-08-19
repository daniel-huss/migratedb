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
package migratedb.v1.testing.util.base

import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * Not a real test engine, just the earliest opportunity to initialize the log system.
 */
class Slf4jInitializer : TestEngine {
    override fun getId() = "SLF4J Initializer"
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId) = EngineDescriptor(uniqueId, id)
    override fun execute(request: ExecutionRequest) {}

    companion object {
        init {
            // Init log system early
            LoggerFactory.getLogger(Slf4jInitializer::class.java)
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
        }
    }
}
