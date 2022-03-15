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

package migratedb.testing

import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

abstract class CoreTest {
    companion object {
        init {
            synchronized(CoreTest::class.java) {
                // Eagerly init the logging system before multi-threading kicks in
                LoggerFactory.getLogger(CoreTest::class.java)
                if (!SLF4JBridgeHandler.isInstalled()) {
                    SLF4JBridgeHandler.removeHandlersForRootLogger()
                    SLF4JBridgeHandler.install()
                }
            }
        }
    }
}
