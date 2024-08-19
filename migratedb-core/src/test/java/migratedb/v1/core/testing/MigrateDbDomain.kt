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

import migratedb.v1.core.api.MigrationPattern
import migratedb.v1.core.api.TargetVersion
import migratedb.v1.core.api.configuration.DefaultConfiguration
import migratedb.v1.core.api.pattern.ValidatePattern
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase

class MigrateDbDomain : DomainContextBase() {
    @Provide
    fun targetVersions(): Arbitrary<TargetVersion> = anyTargetVersion()

    @Provide
    fun migrationPatterns(): Arbitrary<MigrationPattern> = anyMigrationPattern()

    @Provide
    fun validatePatterns(): Arbitrary<ValidatePattern> = anyValidatePattern()

    @Provide
    fun configurations(): Arbitrary<DefaultConfiguration> {
        return Arbitraries.subsetOf(ConfigSetters.all)
            .edgeCases { it.add(ConfigSetters.all.toSet()) }
            .flatMap { subsetOfSetters ->
                Combinators.combine(subsetOfSetters.map { it.asAction }).`as` {
                    it.fold(DefaultConfiguration()) { config, setterAction ->
                        config.also {
                            if (setterAction.precondition(config)) {
                                setterAction.run(config)
                            }
                        }
                    }
                }
            }
    }
}
