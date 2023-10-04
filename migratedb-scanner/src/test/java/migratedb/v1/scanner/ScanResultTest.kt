/*
 * Copyright 2022-2023 The MigrateDB contributors
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

package migratedb.v1.scanner

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

internal class ScanResultTest {
    @Test
    fun `Is unmodifiable`() {
        val mutableSet = mutableSetOf("a")
        val scanResult = ScanResult(mutableSet, mutableSet)
        mutableSet.add("b")
        mutableSet.add("c")
        scanResult.foundClasses.shouldContainExactly("a")
        scanResult.foundResources.shouldContainExactly("a")
        @Suppress("UNCHECKED_CAST")
        shouldThrowAny { (scanResult.foundClasses as MutableCollection<String>).add("") }
    }
}
