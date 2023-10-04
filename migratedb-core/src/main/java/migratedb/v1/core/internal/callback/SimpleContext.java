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
package migratedb.v1.core.internal.callback;

import migratedb.v1.core.api.MigrationInfo;
import migratedb.v1.core.api.callback.Context;
import migratedb.v1.core.api.callback.Error;
import migratedb.v1.core.api.callback.Statement;
import migratedb.v1.core.api.callback.Warning;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Session;
import migratedb.v1.core.api.output.OperationResult;

import java.util.List;

public class SimpleContext implements Context {
    private final Configuration configuration;
    private final Session connection;
    private final MigrationInfo migrationInfo;
    private final Statement statement;
    private final OperationResult operationResult;

    SimpleContext(Configuration configuration, Session connection, MigrationInfo migrationInfo,
                  OperationResult operationResult) {
        this.configuration = configuration;
        this.connection = connection;
        this.migrationInfo = migrationInfo;
        this.operationResult = operationResult;
        this.statement = null;
    }

    public SimpleContext(Configuration configuration, Session connection, MigrationInfo migrationInfo,
                         String sql, List<Warning> warnings, List<Error> errors) {
        this.configuration = configuration;
        this.connection = connection;
        this.migrationInfo = migrationInfo;
        this.operationResult = null;
        this.statement = new SimpleStatement(sql, warnings, errors);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public java.sql.Connection getConnection() {
        return connection.getJdbcConnection();
    }

    @Override
    public MigrationInfo getMigrationInfo() {
        return migrationInfo;
    }

    @Override
    public Statement getStatement() {
        return statement;
    }

    @Override
    public OperationResult getOperationResult() {
        return operationResult;
    }

    private static class SimpleStatement implements Statement {
        private final String sql;
        private final List<Warning> warnings;
        private final List<Error> errors;

        private SimpleStatement(String sql, List<Warning> warnings, List<Error> errors) {
            this.sql = sql;
            this.warnings = warnings;
            this.errors = errors;
        }

        @Override
        public String getSql() {
            return sql;
        }

        @Override
        public List<Warning> getWarnings() {
            return warnings;
        }

        @Override
        public List<Error> getErrors() {
            return errors;
        }
    }
}
