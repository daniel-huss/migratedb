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
package migratedb.core.internal.resolver.sql;

import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.MigrationType;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.callback.Event;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.logging.Log;
import migratedb.core.api.resolver.Context;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.api.resolver.ResolvedMigration;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.parser.PlaceholderReplacingReader;
import migratedb.core.internal.resolver.ChecksumCalculator;
import migratedb.core.internal.resolver.ResolvedMigrationComparator;
import migratedb.core.internal.resolver.ResolvedMigrationImpl;
import migratedb.core.internal.resource.ResourceName;
import migratedb.core.internal.resource.ResourceNameParser;
import migratedb.core.internal.sqlscript.SqlScript;
import migratedb.core.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.core.internal.sqlscript.SqlScriptFactory;

/**
 * Migration resolver for SQL file resources.
 */
public class SqlMigrationResolver implements MigrationResolver {
    private static final Log LOG = Log.getLog(SqlMigrationResolver.class);
    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;
    private final ResourceProvider resourceProvider;
    private final SqlScriptFactory sqlScriptFactory;
    private final Configuration configuration;
    private final ParsingContext parsingContext;

    public SqlMigrationResolver(ResourceProvider resourceProvider, SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                SqlScriptFactory sqlScriptFactory, Configuration configuration,
                                ParsingContext parsingContext) {
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
        this.resourceProvider = resourceProvider;
        this.sqlScriptFactory = sqlScriptFactory;
        this.configuration = configuration;
        this.parsingContext = parsingContext;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();
        String[] suffixes = configuration.getSqlMigrationSuffixes();
        addMigrations(migrations, configuration.getSqlMigrationPrefix(), suffixes, false);
        addMigrations(migrations, configuration.getRepeatableSqlMigrationPrefix(), suffixes, true);
        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }

    private List<Resource> createPlaceholderReplacingResources(List<Resource> resources) {
        List<Resource> list = new ArrayList<>();

        for (Resource resource : resources) {
            Resource placeholderReplacingResource = new Resource() {
                @Override
                public String getName() {
                    return resource.getName();
                }

                @Override
                public Reader read(Charset charset) {
                    return PlaceholderReplacingReader.create(configuration, parsingContext, resource.read(charset));
                }

                @Override
                public String describeLocation() {
                    return resource.describeLocation();
                }

                @Override
                public String toString() {
                    return resource.toString();
                }
            };

            list.add(placeholderReplacingResource);
        }

        return list;
    }

    private int getChecksumForResource(boolean repeatable, List<Resource> resources) {
        if (repeatable && configuration.isPlaceholderReplacement()) {
            return ChecksumCalculator.calculate(createPlaceholderReplacingResources(resources));
        }
        return ChecksumCalculator.calculate(resources);
    }

    private Integer getEquivalentChecksumForResource(boolean repeatable,
                                                     List<Resource> resources) {
        if (repeatable) {
            return ChecksumCalculator.calculate(resources);
        }
        return null;
    }

    private void addMigrations(List<ResolvedMigration> migrations,
                               String prefix,
                               String[] suffixes,
                               boolean repeatable) {
        ResourceNameParser resourceNameParser = new ResourceNameParser(configuration);

        for (Resource resource : resourceProvider.getResources(prefix, suffixes)) {
            String filename = resource.getName();
            ResourceName result = resourceNameParser.parse(filename);
            if (!result.isValid() || isSqlCallback(result) || !prefix.equals(result.getPrefix())) {
                continue;
            }

            SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, configuration.isMixed(), resourceProvider);

            List<Resource> resources = new ArrayList<>();
            resources.add(resource);

            Integer checksum = getChecksumForResource(repeatable, resources);
            Integer equivalentChecksum = getEquivalentChecksumForResource(repeatable, resources);

            migrations.add(new ResolvedMigrationImpl(
                    result.getVersion(),
                    result.getDescription(),
                    resource.getName(),
                    checksum,
                    equivalentChecksum,
                    MigrationType.SQL,
                    resource.getName(),
                    new SqlMigrationExecutor(sqlScriptExecutorFactory, sqlScript, configuration.isBatch())) {
            });
        }
    }

    /**
     * Checks whether this filename is actually a sql-based callback instead of a regular migration.
     *
     * @param result The parsing result to check.
     */
    protected static boolean isSqlCallback(ResourceName result) {
        return Event.fromId(result.getPrefix()) != null;
    }
}
