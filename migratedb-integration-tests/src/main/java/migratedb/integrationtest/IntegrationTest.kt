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

package migratedb.integrationtest

import migratedb.integrationtest.SharedResources.Companion.resources
import migratedb.integrationtest.dsl.Dsl
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import java.util.concurrent.TimeUnit

@ExtendWith(IntegrationTest.Extension::class)
@Timeout(15, unit = TimeUnit.MINUTES)
abstract class IntegrationTest {

    class Extension : BeforeAllCallback {
        companion object {
            private val lock = Any()
            private lateinit var sharedResources: SharedResources
            fun resources() = synchronized(lock) { sharedResources }
        }

        private val namespace = Namespace.create(Extension::class.java)

        override fun beforeAll(context: ExtensionContext) = synchronized(lock) {
            sharedResources = context.root.getStore(namespace).resources()
        }
    }

    fun withDsl(block: (Dsl).() -> (Unit)) = Dsl(Extension.resources()).use(block)
}
