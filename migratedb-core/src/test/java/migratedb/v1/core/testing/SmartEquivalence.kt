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

import com.google.common.base.Equivalence
import java.util.*

// Kotlin compiler does not understand that Equivalence<Any> is valid for Map.diff(), so we have to declare it <Any?> :-/
@Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
object SmartEquivalence : Equivalence<Any?>() {
    override fun doEquivalent(a: Any?, b: Any?): Boolean {
        return when {
            a is Array<*> && b is Array<*> -> elementsEqual(a.toList(), b.toList())
            a is Iterable<*> && b is Iterable<*> -> elementsEqual(a.toList(), b.toList())
            else -> a == b
        }
    }

    private fun elementsEqual(a: List<*>, b: List<*>): Boolean {
        if (a.size == b.size) {
            a.forEachIndexed { index, elem ->
                if (!equivalent(elem, b[index])) return false
            }
            return true
        }
        return false
    }

    override fun doHash(t: Any?): Int {
        return when (t) {
            is Array<*> -> t.contentHashCode()
            else -> t.hashCode()
        }
    }
}
