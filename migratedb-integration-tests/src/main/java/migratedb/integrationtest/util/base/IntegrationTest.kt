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

package migratedb.integrationtest.util.base

import io.kotest.assertions.print.Print
import io.kotest.assertions.print.Printers
import io.kotest.assertions.print.printed
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.util.container.SharedResources
import migratedb.integrationtest.util.container.SharedResources.Companion.resources
import migratedb.integrationtest.util.dsl.Dsl
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.testcontainers.shaded.com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(IntegrationTest.Extension::class)
@Timeout(15, unit = TimeUnit.MINUTES)
abstract class IntegrationTest {
    companion object {
        private val jsonMapper = ObjectMapper().setDefaultPrettyPrinter(DefaultPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)

        private val unclosed: MutableSet<Any> =
            Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

        @JvmStatic
        fun closed(it: Any) = this.also { unclosed.remove(it) }

        @JvmStatic
        fun opened(it: Any) = this.also { unclosed.add(it) }

        @JvmStatic
        fun toClose() = unclosed

        init {
            Printers.add(Any::class, object : Print<Any> {
                @Deprecated(
                    "Use print(a, level) to respect level hints. Deprecated in 5.0.3",
                    ReplaceWith("print(a, 0)")
                )
                override fun print(a: Any) = print(a, 0)
                override fun print(a: Any, level: Int) = when {
                    a.classOverridesObjectToString() -> a.toString().printed()
                    else -> jsonMapper.writeValueAsString(a).printed()
                }
            })
        }

        private fun Any.classOverridesObjectToString(): Boolean {
            return generateSequence(this::class.java) {
                when (it.superclass) {
                    Object::class.java -> null
                    else -> it.superclass
                }
            }.any { runCatching { it.getDeclaredMethod("toString") }.isSuccess }
        }
    }

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

    fun withDsl(dbSystem: DbSystem, block: (Dsl).() -> (Unit)) = Dsl(dbSystem, Extension.resources()).use(block)
}
