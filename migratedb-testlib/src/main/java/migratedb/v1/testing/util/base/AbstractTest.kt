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

import io.kotest.assertions.print.Print
import io.kotest.assertions.print.Printers
import io.kotest.assertions.print.printed
import org.testcontainers.shaded.com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature

abstract class AbstractTest {
    companion object {
        private val jsonMapper = ObjectMapper().setDefaultPrettyPrinter(DefaultPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)

        init {
            Printers.add(Any::class, object : Print<Any> {
                @Suppress("OVERRIDE_DEPRECATION")
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
}
