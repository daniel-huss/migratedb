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

package migratedb.v1.core.testing

import com.google.common.collect.MapDifference.ValueDifference
import com.google.common.collect.Maps
import migratedb.v1.core.api.configuration.Configuration
import kotlin.reflect.KType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun <K, V> Map<K, V>.diffWith(other: Map<*, *>): Map<Any?, ValueDifference<Any?>> =
    Maps.difference(this, other, SmartEquivalence).entriesDiffering()

fun Configuration.comparableProperties(): Map<String, Any> = mutableMapOf<String, Any>().also { props ->
    Configuration::class.functions.asSequence()
        .filter {
            (it.name.startsWith("get") || it.name.startsWith("is")) &&
                    isComparableOrListOfComparable(it.returnType) &&
                    it.parameters.isEmpty()
        }
        .forEach { getter ->
            getter.call(this)?.let { props[getter.name] = it }
        }
}

private fun isComparableOrListOfComparable(type: KType): Boolean {
    return (type.jvmErasure.isSubclassOf(Comparable::class)) ||
            (type.jvmErasure.isSubclassOf(List::class) &&
                    type.arguments.first().type?.jvmErasure?.isSubclassOf(Comparable::class) == true)
}
