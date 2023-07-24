/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.database;

import migratedb.v1.core.api.DatabaseTypeRegister;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.database.base.DatabaseType;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.database.base.BaseDatabaseType;
import migratedb.v1.core.internal.jdbc.JdbcUtils;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatabaseTypeRegisterImpl implements DatabaseTypeRegister {
    private List<DatabaseType> registeredDatabaseTypesInPriorityOrder = Collections.emptyList();

    public void registerDatabaseTypes(Collection<DatabaseType> databaseTypes) {
        synchronized (this) {
            // Copy-on-write
            var copy = new HashSet<DatabaseType>();
            copy.addAll(registeredDatabaseTypesInPriorityOrder);
            copy.addAll(databaseTypes);
            registeredDatabaseTypesInPriorityOrder = copy.stream()
                                                         .sorted(Comparator.comparing(DatabaseType::getPriority))
                                                         .collect(Collectors.toUnmodifiableList());
        }
    }

    public void clear() {
        synchronized (this) {
            registeredDatabaseTypesInPriorityOrder = Collections.emptyList();
        }
    }

    @Override
    public List<DatabaseType> getDatabaseTypes() {
        synchronized (this) {
            return registeredDatabaseTypesInPriorityOrder;
        }
    }

    @Override
    public DatabaseType getDatabaseTypeForConnection(Connection connection) {
        DatabaseMetaData databaseMetaData = JdbcUtils.getDatabaseMetaData(connection);
        String databaseProductName = JdbcUtils.getDatabaseProductName(databaseMetaData);
        String databaseProductVersion = JdbcUtils.getDatabaseProductVersion(databaseMetaData);

        for (DatabaseType type : getDatabaseTypes()) {
            if (type.handlesDatabaseProductNameAndVersion(databaseProductName, databaseProductVersion, connection)) {
                return type;
            }
        }

        throw new MigrateDbException("Unsupported Database: " + databaseProductName);
    }
}
