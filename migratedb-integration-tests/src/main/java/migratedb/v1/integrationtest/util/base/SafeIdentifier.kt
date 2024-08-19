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

package migratedb.v1.integrationtest.util.base

/**
 * A database identifier that doesn't need quoting or escaping .
 */
class SafeIdentifier private constructor(private val s: String) : CharSequence by s {
    companion object {
        private val regex = Regex("""[_a-zA-Z][\w_]{0,29}""")

        fun String.asSafeIdentifier() =
            takeIfSafeIdentifier() ?: throw IllegalArgumentException("Not a safe identifier")

        fun String.takeIfSafeIdentifier() = when (regex.matches(this)) {
            true -> SafeIdentifier(this)
            else -> null
        }
    }

    override fun toString() = s
    override fun equals(other: Any?) = other === this || (other is SafeIdentifier && other.s == s)
    override fun hashCode() = s.hashCode()
}
