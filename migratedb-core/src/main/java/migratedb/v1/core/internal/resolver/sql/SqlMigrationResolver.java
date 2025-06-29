package migratedb.v1.core.internal.resolver.sql;

import migratedb.v1.core.api.Checksum;
import migratedb.v1.core.api.MigrationType;
import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.internal.resource.ResourceName;
import migratedb.v1.core.api.internal.sqlscript.SqlScript;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutorFactory;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptFactory;
import migratedb.v1.core.api.resolver.Context;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.api.resolver.ResolvedMigration;
import migratedb.v1.core.api.resource.Resource;
import migratedb.v1.core.internal.parser.PlaceholderReplacingReader;
import migratedb.v1.core.internal.resolver.ChecksumCalculator;
import migratedb.v1.core.internal.resolver.ResolvedMigrationComparator;
import migratedb.v1.core.internal.resolver.ResolvedMigrationImpl;
import migratedb.v1.core.internal.resource.ResourceNameParser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration resolver for SQL file resources.
 */
public class SqlMigrationResolver implements MigrationResolver {

    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;
    private final ResourceProvider resourceProvider;
    private final SqlScriptFactory sqlScriptFactory;
    private final Configuration configuration;
    private final ParsingContext parsingContext;

    public SqlMigrationResolver(ResourceProvider resourceProvider,
                                SqlScriptExecutorFactory sqlScriptExecutorFactory,
                                SqlScriptFactory sqlScriptFactory,
                                Configuration configuration,
                                ParsingContext parsingContext) {
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
        this.resourceProvider = resourceProvider;
        this.sqlScriptFactory = sqlScriptFactory;
        this.configuration = configuration;
        this.parsingContext = parsingContext;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        var migrations = new ArrayList<ResolvedMigration>();
        var suffixes = configuration.getSqlMigrationSuffixes();
        addMigrations(migrations, configuration.getSqlMigrationPrefix(), suffixes, false, false);
        addMigrations(migrations, configuration.getBaselineMigrationPrefix(), suffixes, false, true);
        addMigrations(migrations, configuration.getRepeatableSqlMigrationPrefix(), suffixes, true, false);
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

    private Checksum getChecksumForResource(boolean repeatable, List<Resource> resources, ResourceName resourceName) {
        if (repeatable && configuration.isPlaceholderReplacement()) {
            parsingContext.updateFilenamePlaceholder(resourceName);
            return ChecksumCalculator.calculate(createPlaceholderReplacingResources(resources), configuration);
        }
        return ChecksumCalculator.calculate(resources, configuration);
    }

    private @Nullable Checksum getEquivalentChecksumForResource(boolean repeatable,
                                                                List<Resource> resources) {
        if (repeatable) {
            return ChecksumCalculator.calculate(resources, configuration);
        }
        return null;
    }

    private void addMigrations(ArrayList<ResolvedMigration> migrations,
                               String prefix,
                               List<String> suffixes,
                               boolean repeatable,
                               boolean baseline) {
        ResourceNameParser resourceNameParser = new ResourceNameParser(configuration);

        for (Resource resource : resourceProvider.getResources(prefix, suffixes)) {
            String filename = resource.getLastNameComponent();
            ResourceName resourceName = resourceNameParser.parse(filename);
            if (!resourceName.isValid() || isSqlCallback(resourceName) || !prefix.equals(resourceName.getPrefix())) {
                continue;
            }

            SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, configuration.isMixed(), resourceProvider);

            List<Resource> resources = new ArrayList<>();
            resources.add(resource);

            var checksum = getChecksumForResource(repeatable, resources, resourceName);
            var equivalentChecksum = getEquivalentChecksumForResource(repeatable, resources);

            migrations.add(new ResolvedMigrationImpl(
                    resourceName.getVersion(),
                    resourceName.getDescription(),
                    resource.getLastNameComponent(),
                    checksum,
                    equivalentChecksum,
                    baseline ? MigrationType.SQL_BASELINE : MigrationType.SQL,
                    resource.describeLocation(),
                    new SqlMigrationExecutor(sqlScriptExecutorFactory, sqlScript)) {
            });
        }
    }

    /**
     * Checks whether this filename is actually a sql-based callback instead of a regular migration.
     *
     * @param result The parsing result to check.
     */
    private static boolean isSqlCallback(ResourceName result) {
        return Event.fromId(result.getPrefix()) != null;
    }
}
