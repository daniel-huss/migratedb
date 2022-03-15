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
package  migratedb.core.api.configuration

import migratedb.testing.CoreTest
import migratedb.testing.MigrateDbDomain
import migratedb.testing.shouldBeEquivalentTo
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.domains.Domain

@Domain(MigrateDbDomain::class)
internal class ClassicConfigurationTest : CoreTest() {
    @Property
    fun `configure() copies all properties except class loader into empty configuration`(
        @ForAll source: ClassicConfiguration
    ) {
        // when
        val target = ClassicConfiguration()
        target.configure(source)

        // then
        target.shouldBeEquivalentTo(source)
    }

    @Property
    fun `configure() copies all properties except class loader into non-empty configuration`(
        @ForAll source: ClassicConfiguration,
        @ForAll target: ClassicConfiguration
    ) {
        // when
        target.configure(source)

        // then
        target.shouldBeEquivalentTo(source)
    }
}
