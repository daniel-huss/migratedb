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

import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.ClassProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.resolver.ResolvedMigrationComparator;
import migratedb.core.internal.util.ClassUtils;

/**
 * Migration resolver for Java-based migrations.
 */
public class JavaMigrationResolver implements MigrationResolver {
    /**
     * The Scanner to use.
     */
    private final ClassProvider<JavaMigration> classProvider;

    /**
     * The configuration to inject (if necessary) in the migration classes.
     */
    private final Configuration configuration;

    /**
     * Creates a new instance.
     *
     * @param classProvider The class provider.
     * @param configuration The configuration to inject (if necessary) in the migration classes.
     */
    public JavaMigrationResolver(ClassProvider<JavaMigration> classProvider, Configuration configuration) {
        this.classProvider = classProvider;
        this.configuration = configuration;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();

        for (Class<?> clazz : classProvider.getClasses()) {
            JavaMigration javaMigration = ClassUtils.instantiate(clazz.getName(), configuration.getClassLoader());
            migrations.add(new ResolvedJavaMigration(javaMigration));
        }

        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }
}
