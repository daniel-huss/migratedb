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

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import migratedb.core.api.configuration.Configuration
import kotlin.reflect.full.functions

fun Configuration.shouldBeEquivalentTo(other: Configuration) {
    Configuration::class.functions.asSequence()
        .filter {
            (it.name.startsWith("get") || it.name.startsWith("is")) &&
                    it.name != "getClassLoader" &&
                    it.name != "getClass" &&
                    it.name != "getDatabaseTypeRegister"
        }
        .forEach {
            val actual = it.call(this)
            val expected = it.call(other)
            "Value of ${it.name}()".asClue {
                actual.shouldBe(expected)
            }
        }
    "Registered database types".asClue {
        databaseTypeRegister.databaseTypes.map { it::class.java }
            .shouldContainExactlyInAnyOrder(other.databaseTypeRegister.databaseTypes.map { it::class.java })
    }
}
