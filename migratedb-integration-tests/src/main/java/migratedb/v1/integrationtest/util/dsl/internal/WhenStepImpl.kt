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

package migratedb.v1.integrationtest.util.dsl.internal

import migratedb.v1.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.v1.integrationtest.util.dsl.*
import migratedb.v1.core.api.MigrationInfoService
import migratedb.v1.core.api.output.BaselineResult
import migratedb.v1.core.api.output.LiberateResult
import migratedb.v1.core.api.output.MigrateResult
import migratedb.v1.core.api.output.RepairResult

class WhenStepImpl<G : Any>(given: G, databaseContext: DatabaseContext) : Dsl.WhenStep<G>,
    AbstractAfterGiven<G>(given, databaseContext) {
    override fun baseline(block: RunBaselineSpec.() -> Unit): BaselineResult {
        val runBaseline = RunBaselineImpl(databaseContext)
        runBaseline.block()
        return runBaseline.execute()
    }

    override fun migrate(block: (RunMigrateSpec).() -> Unit): MigrateResult {
        val runMigrate = RunMigrateImpl(databaseContext)
        runMigrate.block()
        return runMigrate.execute()
    }

    override fun info(block: (RunInfoSpec).() -> Unit): MigrationInfoService {
        val runInfo = RunInfoImpl(databaseContext)
        runInfo.block()
        return runInfo.execute()
    }

    override fun repair(block: RunRepairSpec.() -> Unit): RepairResult {
        val runRepair = RunRepairImpl(databaseContext)
        runRepair.block()
        return runRepair.execute()
    }

    override fun liberate(block: RunLiberateSpec.() -> Unit): LiberateResult {
        val runLiberate = RunLiberateImpl(databaseContext)
        runLiberate.block()
        return runLiberate.execute()
    }

    override fun arbitraryMutation(): IndependentDatabaseMutation {
        return databaseContext.databaseInstance.nextMutation(databaseContext.schemaName)
    }

    override fun <T> justRun(block: JustRun.() -> T): T {
        return JustRunImpl(databaseContext).block()
    }
}
