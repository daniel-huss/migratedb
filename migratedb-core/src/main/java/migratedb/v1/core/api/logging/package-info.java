/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
/**
 * MigrateDB's log abstraction. Custom MigrationResolver, MigrationExecutor, MigrateDbCallback, ErrorHandler and
 * JdbcMigration implementations should use this to obtain a logger that obeys the logging configuration passed to
 * MigrateDB.
 */
@DefaultQualifier(value = NonNull.class, locations = { TypeUseLocation.PARAMETER, TypeUseLocation.RETURN })
package migratedb.v1.core.api.logging;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
