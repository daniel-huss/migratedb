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

import migratedb.core.api.MigrationPattern
import migratedb.core.api.MigrationVersion
import migratedb.core.api.configuration.ClassicConfiguration
import migratedb.core.api.pattern.ValidatePattern
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase

class MigrateDbDomain : DomainContextBase() {

    @Provide
    fun migrationVersions(): Arbitrary<MigrationVersion> = anyMigrationVersion()

    @Provide
    fun migrationPatterns(): Arbitrary<MigrationPattern> = anyMigrationPattern()

    @Provide
    fun validatePatterns(): Arbitrary<ValidatePattern> = anyValidatePattern()

    @Provide
    fun configurations(): Arbitrary<ClassicConfiguration> {
        return Arbitraries.subsetOf(ConfigSetters.all)
            .edgeCases { it.add(ConfigSetters.all.toSet()) }
            .flatMap { subset ->
                Combinators.combine(subset.map { it.asAction }).`as` {
                    it.fold(ClassicConfiguration()) { config, setterAction ->
                        if (setterAction.precondition(config)) {
                            setterAction.run(config)
                        }
                        config
                    }
                }
            }
    }
}
