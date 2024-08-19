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
package migratedb.v1.core.internal.resolver.java;

import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.migration.JavaMigration;
import migratedb.v1.core.api.resolver.Context;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.internal.resolver.ResolvedMigrationComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static migratedb.v1.core.internal.resolver.java.JavaMigrationResolver.newResolvedJavaMigration;

/**
 * Migration resolver for a fixed set of pre-instantiated Java-based migrations.
 */
public class FixedJavaMigrationResolver implements MigrationResolver {
    /**
     * The JavaMigration instances to use.
     */
    private final Collection<JavaMigration> javaMigrations;
    private final SqlScriptFactory sqlScriptFactory;
    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;

    public FixedJavaMigrationResolver(SqlScriptFactory sqlScriptFactory,
                                      SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                      Collection<JavaMigration> javaMigrations) {
        this.sqlScriptFactory = sqlScriptFactory;
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
        this.javaMigrations = javaMigrations;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();

        for (JavaMigration javaMigration : javaMigrations) {
            migrations.add(newResolvedJavaMigration(javaMigration,
                                                    context.getConfiguration(),
                                                    sqlScriptFactory,
                                                    sqlScriptExecutorFactory
            ));
        }

        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }
}
