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

import migratedb.v1.core.api.internal.database.base.Database
import migratedb.v1.integrationtest.database.DbSystem
import migratedb.v1.integrationtest.util.base.SafeIdentifier
import javax.sql.DataSource

data class DatabaseContext(
    val databaseInstance: DbSystem.Instance,
    val adminDataSource: DataSource,
    val database: Database,
    val namespace: SafeIdentifier,
    val schemaName: SafeIdentifier?,
)
