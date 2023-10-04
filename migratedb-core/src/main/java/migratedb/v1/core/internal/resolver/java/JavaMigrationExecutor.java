/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.internal.resolver.java;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.executor.Context;
import migratedb.v1.core.api.executor.MigrationExecutor;
import migratedb.v1.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.v1.core.api.internal.database.base.DatabaseType;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.migration.JavaMigration;
import migratedb.v1.core.internal.resource.ReaderResource;

import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Adapter for executing migrations implementing JavaMigration.
 */
public class JavaMigrationExecutor implements MigrationExecutor {
    private final JavaMigration javaMigration;
    private final SqlScriptFactory sqlScriptFactory;
    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;

    JavaMigrationExecutor(JavaMigration javaMigration,
                          SqlScriptFactory sqlScriptFactory,
                          SqlScriptExecutorFactory sqlScriptExecutorFactory) {
        this.javaMigration = javaMigration;
        this.sqlScriptFactory = sqlScriptFactory;
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
    }

    @Override
    public void execute(Context context) throws SQLException {
        DatabaseType databaseType = context.getConfiguration()
                .getDatabaseTypeRegister()
                .getDatabaseTypeForConnection(context.getConnection());

        DatabaseExecutionStrategy strategy = databaseType.createExecutionStrategy(context.getConnection());
        strategy.execute(() -> {
            executeOnce(context);
            return true;
        });
    }

    private void executeOnce(Context context) throws SQLException {
        try {
            javaMigration.migrate(new migratedb.v1.core.api.migration.Context() {
                @Override
                public Configuration getConfiguration() {
                    return context.getConfiguration();
                }

                @Override
                public Connection getConnection() {
                    return context.getConnection();
                }

                @Override
                public void runScript(Reader script) {
                    var resource = new ReaderResource("script", script);
                    var sqlScript = sqlScriptFactory.createSqlScript(resource, context.getConfiguration().isMixed(), ResourceProvider.noResources());
                    sqlScriptExecutorFactory.createSqlScriptExecutor(getConnection(), getConfiguration().isOutputQueryResults())
                            .execute(sqlScript);
                }
            });
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new MigrateDbException("Migration failed !", e);
        }
    }

    @Override
    public boolean canExecuteInTransaction() {
        return javaMigration.canExecuteInTransaction();
    }

    @Override
    public boolean shouldExecute() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "javaMigration=" + javaMigration +
                '}';
    }
}
