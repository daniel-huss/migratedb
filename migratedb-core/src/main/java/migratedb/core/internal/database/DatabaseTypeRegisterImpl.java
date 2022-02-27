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
package migratedb.core.internal.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import migratedb.core.api.DatabaseTypeRegister;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.database.base.BaseDatabaseType;
import migratedb.core.internal.jdbc.JdbcUtils;
import migratedb.core.internal.util.StringUtils;

public class DatabaseTypeRegisterImpl implements DatabaseTypeRegister {
    private static final Log LOG = Log.getLog(DatabaseTypeRegister.class);

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

    @Override
    public List<DatabaseType> getDatabaseTypes() {
        synchronized (this) {
            return registeredDatabaseTypesInPriorityOrder;
        }
    }

    @Override
    public DatabaseType getDatabaseTypeForUrl(String url) {
        List<DatabaseType> typesAcceptingUrl = getDatabaseTypesForUrl(url);

        if (typesAcceptingUrl.size() > 0) {
            if (typesAcceptingUrl.size() > 1) {
                StringBuilder builder = new StringBuilder();
                for (DatabaseType type : typesAcceptingUrl) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(type.getName());
                }

                LOG.debug("Multiple databases found that handle url '" + redactJdbcUrl(url) + "': " + builder);
            }
            return typesAcceptingUrl.get(0);
        } else {
            throw new MigrateDbException("No database found to handle " + redactJdbcUrl(url));
        }
    }

    private List<DatabaseType> getDatabaseTypesForUrl(String url) {
        List<DatabaseType> typesAcceptingUrl = new ArrayList<>();
        for (DatabaseType type : getDatabaseTypes()) {
            if (type.handlesJDBCUrl(url)) {
                typesAcceptingUrl.add(type);
            }
        }

        return typesAcceptingUrl;
    }

    @Override
    public String redactJdbcUrl(String url) {
        List<DatabaseType> types = getDatabaseTypesForUrl(url);
        if (types.isEmpty()) {
            url = redactJdbcUrl(url, BaseDatabaseType.getDefaultJDBCCredentialsPattern());
        } else {
            for (DatabaseType type : types) {
                Pattern dbPattern = type.getJDBCCredentialsPattern();
                if (dbPattern != null) {
                    url = redactJdbcUrl(url, dbPattern);
                }
            }
        }
        return url;
    }

    private String redactJdbcUrl(String url, Pattern pattern) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String password = matcher.group(1);
            return url.replace(password, StringUtils.trimOrPad("", password.length(), '*'));
        }
        return url;
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
