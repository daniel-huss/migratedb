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
package migratedb.core.internal.resolver.java;

import java.sql.Connection;
import java.sql.SQLException;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.executor.Context;
import migratedb.core.api.executor.MigrationExecutor;
import migratedb.core.api.internal.database.DatabaseExecutionStrategy;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.migration.JavaMigration;

/**
 * Adapter for executing migrations implementing JavaMigration.
 */
public class JavaMigrationExecutor implements MigrationExecutor {
    /**
     * The JavaMigration to execute.
     */
    private final JavaMigration javaMigration;

    /**
     * Creates a new JavaMigrationExecutor.
     *
     * @param javaMigration The JavaMigration to execute.
     */
    JavaMigrationExecutor(JavaMigration javaMigration) {
        this.javaMigration = javaMigration;
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
            javaMigration.migrate(new migratedb.core.api.migration.Context() {
                @Override
                public Configuration getConfiguration() {
                    return context.getConfiguration();
                }

                @Override
                public Connection getConnection() {
                    return context.getConnection();
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
