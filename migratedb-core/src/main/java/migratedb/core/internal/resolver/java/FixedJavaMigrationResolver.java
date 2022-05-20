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

import static migratedb.core.internal.resolver.java.JavaMigrationResolver.newResolvedJavaMigration;

import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.internal.resolver.ResolvedMigrationComparator;

/**
 * Migration resolver for a fixed set of pre-instantiated Java-based migrations.
 */
public class FixedJavaMigrationResolver implements MigrationResolver {
    /**
     * The JavaMigration instances to use.
     */
    private final JavaMigration[] javaMigrations;

    /**
     * Creates a new instance.
     *
     * @param javaMigrations The JavaMigration instances to use.
     */
    public FixedJavaMigrationResolver(JavaMigration... javaMigrations) {
        this.javaMigrations = javaMigrations;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();

        for (JavaMigration javaMigration : javaMigrations) {
            migrations.add(newResolvedJavaMigration(javaMigration, context.getConfiguration()));
        }

        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }
}
