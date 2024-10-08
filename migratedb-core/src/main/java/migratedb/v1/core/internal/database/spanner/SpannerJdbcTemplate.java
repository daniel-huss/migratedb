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
package migratedb.v1.core.internal.database.spanner;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.database.base.DatabaseType;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.jdbc.JdbcNullTypes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SpannerJdbcTemplate extends JdbcTemplate {

    public SpannerJdbcTemplate(Connection connection, DatabaseType databaseType) {
        super(connection, databaseType);
    }

    @Override
    protected PreparedStatement prepareStatement(String sql, Object[] params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        // Spanner requires specific types for NULL according to the column.
        // This is unlike other databases which have a single "null type".
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                statement.setNull(i + 1, nullType);
            } else if (params[i] instanceof Integer) {
                statement.setInt(i + 1, (Integer) params[i]);
            } else if (params[i] instanceof Boolean) {
                statement.setBoolean(i + 1, (Boolean) params[i]);
            } else if (params[i] instanceof String) {
                statement.setString(i + 1, params[i].toString());
            } else if (params[i] == JdbcNullTypes.StringNull) {
                statement.setNull(i + 1, Types.NVARCHAR);
            } else if (params[i] == JdbcNullTypes.IntegerNull) {
                statement.setNull(i + 1, Types.INTEGER);
            } else if (params[i] == JdbcNullTypes.BooleanNull) {
                statement.setNull(i + 1, Types.BOOLEAN);
            } else {
                throw new MigrateDbException("Unhandled object of type '" + params[i].getClass().getName() + "'. ");
            }
        }

        return statement;
    }
}
