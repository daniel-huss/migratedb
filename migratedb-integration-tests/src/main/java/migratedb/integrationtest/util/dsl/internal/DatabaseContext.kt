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

package migratedb.integrationtest.util.dsl.internal

import migratedb.core.api.internal.database.base.Database
import migratedb.integrationtest.database.DbSystem
import migratedb.integrationtest.util.base.SafeIdentifier
import javax.sql.DataSource

data class DatabaseContext(
    val databaseHandle: DbSystem.Handle,
    val adminDataSource: DataSource,
    val database: Database<*>,
    val namespace: SafeIdentifier,
    val schemaName: SafeIdentifier?,
)
