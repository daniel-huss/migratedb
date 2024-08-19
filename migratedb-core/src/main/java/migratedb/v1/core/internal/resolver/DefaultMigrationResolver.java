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
package migratedb.v1.core.internal.resolver;

import migratedb.v1.core.api.ClassProvider;
import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.migration.JavaMigration;
import migratedb.v1.core.api.resolver.Context;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.internal.resolver.java.FixedJavaMigrationResolver;
import migratedb.v1.core.internal.resolver.java.JavaMigrationResolver;
import migratedb.v1.core.internal.resolver.sql.SqlMigrationResolver;

import java.util.*;

/**
 * Implements the default MigrateDB behavior, which combines the various migration sources that can be configured.
 */
public class DefaultMigrationResolver implements MigrationResolver {
    /**
     * The migration resolvers to use internally.
     */
    private final Collection<MigrationResolver> migrationResolvers = new ArrayList<>();
    /**
     * The available migrations, sorted by version, newest first. An empty list is returned when no migrations can be
     * found.
     */
    private List<ResolvedMigration> availableMigrations;

    public DefaultMigrationResolver(ResourceProvider resourceProvider,
                                    ClassProvider<JavaMigration> classProvider,
                                    Configuration configuration,
                                    SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                    SqlScriptFactory sqlScriptFactory,
                                    ParsingContext parsingContext,
                                    Collection<MigrationResolver> customMigrationResolvers
    ) {
        if (!configuration.isSkipDefaultResolvers()) {
            migrationResolvers.add(new SqlMigrationResolver(resourceProvider,
                                                            sqlScriptExecutorFactory,
                                                            sqlScriptFactory,
                                                            configuration,
                                                            parsingContext));
            migrationResolvers.add(new JavaMigrationResolver(classProvider, sqlScriptFactory, sqlScriptExecutorFactory));

        }
        migrationResolvers.add(new FixedJavaMigrationResolver(sqlScriptFactory,
                                                              sqlScriptExecutorFactory,
                                                              configuration.getJavaMigrations()));

        migrationResolvers.addAll(customMigrationResolvers);
    }

    /**
     * Finds all available migrations using all migration resolvers (sql, java, ...).
     *
     * @return The available migrations, sorted by version, oldest first. An empty list is returned when no migrations
     * can be found.
     * @throws MigrateDbException when the available migrations have overlapping versions.
     */
    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        if (availableMigrations == null) {
            availableMigrations = doFindAvailableMigrations(context);
        }

        return availableMigrations;
    }

    private List<ResolvedMigration> doFindAvailableMigrations(Context context) throws MigrateDbException {
        List<ResolvedMigration> migrations = new ArrayList<>(collectMigrations(migrationResolvers, context));
        migrations.sort(new ResolvedMigrationComparator());

        checkForIncompatibilities(migrations);

        return migrations;
    }

    /**
     * Collects all the migrations for all migration resolvers.
     *
     * @param migrationResolvers The migration resolvers to check.
     * @return All migrations.
     */
    static Collection<ResolvedMigration> collectMigrations(Collection<MigrationResolver> migrationResolvers,
                                                           Context context) {
        Set<ResolvedMigration> migrations = new HashSet<>();
        for (MigrationResolver migrationResolver : migrationResolvers) {
            migrations.addAll(migrationResolver.resolveMigrations(context));
        }
        return migrations;
    }

    /**
     * Checks for incompatible migrations.
     *
     * @param migrations The migrations to check.
     * @throws MigrateDbException when two different migration with the same version number are found.
     */
    static void checkForIncompatibilities(List<ResolvedMigration> migrations) {
        ResolvedMigrationComparator resolvedMigrationComparator = new ResolvedMigrationComparator();
        // check for more than one migration with same version
        for (int i = 0; i < migrations.size() - 1; i++) {
            ResolvedMigration current = migrations.get(i);
            ResolvedMigration next = migrations.get(i + 1);
            if (resolvedMigrationComparator.compare(current, next) == 0) {
                if (current.getType().isBaselineMigration() ^ next.getType().isBaselineMigration()) {
                    continue;
                }
                if (current.getVersion() != null) {
                    throw new MigrateDbException(
                            "Found more than one migration with version " + current.getVersion() + "\nOffenders:\n-> " +
                            current.getLocationDescription() + " (" + current.getType() + ")\n-> " +
                            next.getLocationDescription() + " (" + next.getType() + ")",
                            ErrorCode.DUPLICATE_VERSIONED_MIGRATION);
                }
                throw new MigrateDbException(
                        "Found more than one repeatable migration with description " + current.getDescription() +
                        "\nOffenders:\n-> " + current.getLocationDescription() + " (" + current.getType() + ")\n-> " +
                        next.getLocationDescription() + " (" + next.getType() + ")",
                        ErrorCode.DUPLICATE_REPEATABLE_MIGRATION);
            }
        }
    }
}
