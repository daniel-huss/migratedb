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

import migratedb.core.api.ClassProvider;
import migratedb.core.api.MigrationType;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.resolver.ResolvedMigrationComparator;
import migratedb.core.internal.resolver.ResolvedMigrationImpl;
import migratedb.core.internal.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Migration resolver for Java-based migrations.
 */
public class JavaMigrationResolver implements MigrationResolver {
    /**
     * Creates a new ResolvedJavaMigration based on a {@link JavaMigration}.
     */
    public static ResolvedMigration newResolvedJavaMigration(JavaMigration javaMigration,
                                                             Configuration configuration,
                                                             SqlScriptFactory sqlScriptFactory,
                                                             SqlScriptExecutorFactory sqlScriptExecutorFactory) {
        return new ResolvedMigrationImpl(javaMigration.getVersion(),
                                         javaMigration.getDescription(),
                                         javaMigration.getClass().getName(),
                                         javaMigration.getChecksum(configuration),
                                         null,
                                         javaMigration.isBaselineMigration() ? MigrationType.JDBC_BASELINE
                                                 : MigrationType.JDBC,
                                         String.valueOf(ClassUtils.guessLocationOnDisk(javaMigration.getClass())),
                                         new JavaMigrationExecutor(javaMigration, sqlScriptFactory, sqlScriptExecutorFactory)
        );
    }

    /**
     * The Scanner to use.
     */
    private final ClassProvider<JavaMigration> classProvider;
    private final SqlScriptFactory sqlScriptFactory;
    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;

    /**
     * Creates a new instance.
     *
     * @param classProvider The class provider.
     */
    public JavaMigrationResolver(ClassProvider<JavaMigration> classProvider,
                                 SqlScriptFactory sqlScriptFactory,
                                 SqlScriptExecutorFactory sqlScriptExecutorFactory) {
        this.classProvider = classProvider;
        this.sqlScriptFactory = sqlScriptFactory;
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();

        for (Class<?> clazz : classProvider.getClasses()) {
            JavaMigration javaMigration = ClassUtils.instantiate(clazz.getName(),
                                                                 context.getConfiguration().getClassLoader());
            migrations.add(newResolvedJavaMigration(javaMigration, context.getConfiguration(), sqlScriptFactory, sqlScriptExecutorFactory));
        }

        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }
}
