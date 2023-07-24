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
package migratedb.v1.core.api.configuration;

import migratedb.v1.core.api.*;
import migratedb.v1.core.api.callback.Callback;
import migratedb.v1.core.api.logging.LogSystem;
import migratedb.v1.core.api.logging.LogSystems;
import migratedb.v1.core.api.migration.JavaMigration;
import migratedb.v1.core.api.pattern.ValidatePattern;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.internal.configuration.ConfigUtils;
import migratedb.v1.core.internal.database.DatabaseTypeRegisterImpl;
import migratedb.v1.core.internal.extension.BuiltinFeatures;
import migratedb.v1.core.internal.util.ClassUtils;
import migratedb.v1.core.internal.util.Locations;
import migratedb.v1.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaBean-style configuration for MigrateDB. This configuration can then be passed to MigrateDB using the
 * {@code new MigrateDb(Configuration)} constructor.
 */
public class DefaultConfiguration implements Configuration {
    private @Nullable ConnectionProvider dataSource;
    private int connectRetries;
    private int connectRetriesInterval = 120;
    private String initSql;
    private final ClassLoader classLoader;
    private Locations locations;
    private Charset encoding = StandardCharsets.UTF_8;
    private String defaultSchemaName = null;
    private List<String> schemas = List.of();
    private String table = "migratedb_state";
    private String oldTable = "flyway_schema_history";
    private boolean liberateOnMigrate = true;
    private @Nullable String tablespace;
    private TargetVersion target;
    private boolean failOnMissingTarget = true;
    private List<MigrationPattern> cherryPick = List.of();
    private boolean placeholderReplacement = true;
    private Map<String, String> placeholders = new HashMap<>();
    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private String scriptPlaceholderPrefix = "FP__";
    private String scriptPlaceholderSuffix = "__";
    private String sqlMigrationPrefix = "V";
    private String baselineMigrationPrefix = "B";
    private String repeatableSqlMigrationPrefix = "R";
    private ResourceProvider resourceProvider = null;
    private ClassProvider<JavaMigration> javaMigrationClassProvider = null;
    private String sqlMigrationSeparator = "__";
    private List<String> sqlMigrationSuffixes = List.of(".sql");
    private List<JavaMigration> javaMigrations = List.of();
    private boolean ignoreMissingMigrations;
    private boolean ignoreIgnoredMigrations;
    private boolean ignorePendingMigrations;
    private boolean ignoreFutureMigrations = true;
    private List<ValidatePattern> ignoreMigrationPatterns = List.of();
    private boolean validateMigrationNaming = false;
    private boolean validateOnMigrate = true;
    private Version baselineVersion = Version.parse("1");
    private String baselineDescription = "<< MigrateDB Baseline >>";
    private boolean baselineOnMigrate;
    private boolean outOfOrder;
    private boolean skipExecutingMigrations;
    private List<Callback> callbacks = List.of();
    private boolean skipDefaultCallbacks;
    private List<MigrationResolver> resolvers = List.of();
    private boolean skipDefaultResolvers;
    private boolean mixed;
    private boolean group;
    private String installedBy;
    private boolean createSchemas = true;
    private boolean outputQueryResults = false;
    private int lockRetryCount = 50;
    private boolean failOnMissingLocations = false;
    private LogSystem logger;
    private final DatabaseTypeRegisterImpl databaseTypeRegister = new DatabaseTypeRegisterImpl();
    private final Set<MigrateDbExtension> loadedExtensions = new HashSet<>();
    private final Map<Class<? extends ExtensionConfig>, ExtensionConfig> extensionConfig = new HashMap<>();

    public DefaultConfiguration() {
        this(ClassUtils.defaultClassLoader());
    }

    /**
     * @param classLoader The ClassLoader to use for loading migrations, resolvers, etc. from the classpath. (default:
     *                    Thread.currentThread().getContextClassLoader()). Nullable for compatibility.
     */
    public DefaultConfiguration(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader == null ? ClassUtils.defaultClassLoader() : classLoader;
        this.locations = new Locations(List.of("db/migration"), classLoader);
        useExtension(BuiltinFeatures.instance());
    }

    /**
     * Creates a new configuration with the same values as this existing one.
     */
    public DefaultConfiguration(Configuration configuration) {
        this(configuration.getClassLoader());
        configure(configuration);
    }

    /**
     * @return Fluent interface that delegates to this configuration instance.
     */
    public FluentConfiguration asFluentConfiguration() {
        return new FluentConfiguration(this);
    }

    @Override
    public List<Location> getLocations() {
        return locations.getLocations();
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public String getDefaultSchema() {
        return defaultSchemaName;
    }

    @Override
    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public String getOldTable() {
        return oldTable;
    }

    @Override
    public boolean isLiberateOnMigrate() {
        return liberateOnMigrate;
    }

    @Override
    public @Nullable String getTablespace() {
        return tablespace;
    }

    @Override
    public TargetVersion getTarget() {
        return target;
    }

    @Override
    public boolean isFailOnMissingTarget() {
        return failOnMissingTarget;
    }

    @Override
    public List<MigrationPattern> getCherryPick() {
        return cherryPick;
    }

    @Override
    public boolean isPlaceholderReplacement() {
        return placeholderReplacement;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    @Override
    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    @Override
    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    @Override
    public String getScriptPlaceholderPrefix() {
        return scriptPlaceholderPrefix;
    }

    @Override
    public String getScriptPlaceholderSuffix() {
        return scriptPlaceholderSuffix;
    }

    @Override
    public String getSqlMigrationPrefix() {
        return sqlMigrationPrefix;
    }

    @Override
    public String getBaselineMigrationPrefix() {
        return baselineMigrationPrefix;
    }

    @Override
    public String getRepeatableSqlMigrationPrefix() {
        return repeatableSqlMigrationPrefix;
    }

    @Override
    public String getSqlMigrationSeparator() {
        return sqlMigrationSeparator;
    }

    @Override
    public List<String> getSqlMigrationSuffixes() {
        return sqlMigrationSuffixes;
    }

    @Override
    public List<JavaMigration> getJavaMigrations() {
        return javaMigrations;
    }

    @Override
    public boolean isIgnoreMissingMigrations() {
        return ignoreMissingMigrations;
    }

    @Override
    public boolean isIgnoreIgnoredMigrations() {
        return ignoreIgnoredMigrations;
    }

    @Override
    public boolean isIgnorePendingMigrations() {
        return ignorePendingMigrations;
    }

    @Override
    public boolean isIgnoreFutureMigrations() {
        return ignoreFutureMigrations;
    }

    @Override
    public List<ValidatePattern> getIgnoreMigrationPatterns() {
        return ignoreMigrationPatterns;
    }

    @Override
    public boolean isValidateMigrationNaming() {
        return validateMigrationNaming;
    }

    @Override
    public boolean isValidateOnMigrate() {
        return validateOnMigrate;
    }

    @Override
    public Version getBaselineVersion() {
        return baselineVersion;
    }

    @Override
    public String getBaselineDescription() {
        return baselineDescription;
    }

    @Override
    public boolean isBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    @Override
    public boolean isOutOfOrder() {
        return outOfOrder;
    }

    @Override
    public boolean isSkipExecutingMigrations() {
        return skipExecutingMigrations;
    }

    @Override
    public List<MigrationResolver> getResolvers() {
        return resolvers;
    }

    @Override
    public boolean isSkipDefaultResolvers() {
        return skipDefaultResolvers;
    }

    @Override
    public @Nullable ConnectionProvider getDataSource() {
        return dataSource;
    }

    @Override
    public int getConnectRetries() {
        return connectRetries;
    }

    @Override
    public int getConnectRetriesInterval() {
        return connectRetriesInterval;
    }

    @Override
    public String getInitSql() {
        return initSql;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public boolean isMixed() {
        return mixed;
    }

    @Override
    public String getInstalledBy() {
        return installedBy;
    }

    @Override
    public boolean isGroup() {
        return group;
    }

    @Override
    public int getLockRetryCount() {
        return lockRetryCount;
    }

    @Override
    public LogSystem getLogger() {
        return logger;
    }

    @Override
    public boolean isFailOnMissingLocations() {
        return failOnMissingLocations;
    }

    @Override
    public boolean isOutputQueryResults() {
        return outputQueryResults;
    }

    @Override
    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    @Override
    public ClassProvider<JavaMigration> getJavaMigrationClassProvider() {
        return javaMigrationClassProvider;
    }

    @Override
    public boolean isCreateSchemas() {
        return createSchemas;
    }

    @Override
    public List<Callback> getCallbacks() {
        return callbacks;
    }

    @Override
    public boolean isSkipDefaultCallbacks() {
        return skipDefaultCallbacks;
    }

    @Override
    public DatabaseTypeRegister getDatabaseTypeRegister() {
        return databaseTypeRegister;
    }

    @Override
    public Set<MigrateDbExtension> getLoadedExtensions() {
        return Collections.unmodifiableSet(loadedExtensions);
    }

    @Override
    public Map<Class<? extends ExtensionConfig>, ? extends ExtensionConfig> getExtensionConfig() {
        return Collections.unmodifiableMap(extensionConfig);
    }

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for
     * databases with support for DDL transactions).
     */
    public void setGroup(boolean group) {
        this.group = group;
    }

    /**
     * The username that will be recorded in the schema history table as having applied the migration.
     */
    public void setInstalledBy(String installedBy) {
        if ("".equals(installedBy)) {
            installedBy = null;
        }
        this.installedBy = installedBy;
    }

    /**
     * The log system MigrateDB should use.
     */
    public void setLogger(LogSystem logger) {
        this.logger = logger;
    }

    /**
     * The log system(s) MigrateDB should use.
     */
    public void setLogger(String... logger) {
        this.logger = LogSystems.fromStrings(new LinkedHashSet<>(Arrays.asList(logger)), getClassLoader(), null);
    }

    /**
     * Whether to allow mixing transactional and non-transactional statements within the same migration. Enabling this
     * automatically causes the entire affected migration to be run without a transaction.
     * <p>
     * Note that this is only applicable for PostgreSQL, Aurora PostgreSQL, SQL Server and SQLite which all have
     * statements that do not run at all within a transaction. This is not to be confused with implicit transaction, as
     * they occur in MySQL or Oracle, where even though a DDL statement was run within a transaction, the database will
     * issue an implicit commit before and after its execution.
     */
    public void setMixed(boolean mixed) {
        this.mixed = mixed;
    }

    /**
     * Ignore missing migrations when reading the schema history table. These are migrations that were performed by an
     * older deployment of the application that are no longer available in this version. For example: we have migrations
     * available on the classpath with versions 1.0 and 3.0. The schema history table indicates that a migration with
     * version 2.0 (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDB continues normally. This is useful for situations where one must be able to
     * deploy a newer version of the application even though it doesn't contain migrations included with an older one
     * anymore. Note that if the most recently applied migration is removed, MigrateDB has no way to know it is missing
     * and will mark it as future instead.
     */
    public void setIgnoreMissingMigrations(boolean ignoreMissingMigrations) {
        this.ignoreMissingMigrations = ignoreMissingMigrations;
    }

    /**
     * Ignore ignored migrations when reading the schema history table. These are migrations that were added in between
     * already migrated migrations in this version. For example: we have migrations available on the classpath with
     * versions from 1.0 to 3.0. The schema history table indicates that version 1 was finished on 1.0.15, and the next
     * one was 2.0.0. But with the next release a new migration was added to version 1: 1.0.16. Such scenario is ignored
     * by migrate command, but by default is rejected by validate. When ignoreIgnoredMigrations is enabled, such case
     * will not be reported by validate command. This is useful for situations where one must be able to deliver
     * complete set of migrations in a delivery package for multiple versions of the product, and allows for further
     * development of older versions.
     */
    public void setIgnoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
        this.ignoreIgnoredMigrations = ignoreIgnoredMigrations;
    }

    /**
     * Ignore pending migrations when reading the schema history table. These are migrations that are available but have
     * not yet been applied. This can be useful for verifying that in-development migration changes don't contain any
     * validation-breaking changes of migrations that have already been applied to a production environment, e.g. as
     * part of a CI/CD process, without failing because of the existence of new migration versions.
     */
    public void setIgnorePendingMigrations(boolean ignorePendingMigrations) {
        this.ignorePendingMigrations = ignorePendingMigrations;
    }

    /**
     * Whether to ignore future migrations when reading the schema history table. These are migrations that were
     * performed by a newer deployment of the application that are not yet available in this version. For example: we
     * have migrations available on the classpath up to version 3.0. The schema history table indicates that a migration
     * to version 4.0 (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDB continues normally. This is useful for situations where one must be able to
     * redeploy an older version of the application after the database has been migrated by a newer one.
     */
    public void setIgnoreFutureMigrations(boolean ignoreFutureMigrations) {
        this.ignoreFutureMigrations = ignoreFutureMigrations;
    }

    /**
     * Ignore migrations that match this list of patterns when validating migrations. Each pattern is of the form
     * {@code <migration_type>:<migration_state>}. See <a
     * href="https://daniel-huss.github.io/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns">the
     * website</a> for full details.
     * <p>Example: repeatable:missing,versioned:pending,*:failed
     */
    public void setIgnoreMigrationPatternsAsStrings(String... ignoreMigrationPatterns) {
        setIgnoreMigrationPatternsAsStrings(Arrays.asList(ignoreMigrationPatterns));
    }

    /**
     * Ignore migrations that match this list of patterns when validating migrations. Each pattern is of the form
     * {@code <migration_type>:<migration_state>}. See <a
     * href="https://daniel-huss.github.io/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns">the
     * website</a> for full details.
     * <p>Example: repeatable:missing,versioned:pending,*:failed
     */
    public void setIgnoreMigrationPatternsAsStrings(Collection<String> ignoreMigrationPatterns) {
        this.ignoreMigrationPatterns = ignoreMigrationPatterns.stream()
                                                              .map(ValidatePattern::fromPattern)
                                                              .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Ignore migrations that match the given ValidatePatterns when validating migrations.
     */
    public void setIgnoreMigrationPatterns(ValidatePattern... ignoreMigrationPatterns) {
        setIgnoreMigrationPatterns(Arrays.asList(ignoreMigrationPatterns));
    }

    /**
     * Ignore migrations that match the given ValidatePatterns when validating migrations.
     */
    public void setIgnoreMigrationPatterns(Collection<ValidatePattern> ignoreMigrationPatterns) {
        this.ignoreMigrationPatterns = List.copyOf(ignoreMigrationPatterns);
    }

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure
     * can be useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     */
    public void setValidateMigrationNaming(boolean validateMigrationNaming) {
        this.validateMigrationNaming = validateMigrationNaming;
    }

    /**
     * Whether to automatically call validate or not when running migrate.
     */
    public void setValidateOnMigrate(boolean validateOnMigrate) {
        this.validateOnMigrate = validateOnMigrate;
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     */
    public void setLocationsAsStrings(String... locations) {
        setLocationsAsStrings(Arrays.asList(locations));
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     */
    public void setLocationsAsStrings(Collection<String> locations) {
        this.locations = new Locations(List.copyOf(locations), classLoader);
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     */
    public void setLocations(Location... locations) {
        setLocations(Arrays.asList(locations));
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     */
    public void setLocations(Collection<Location> locations) {
        this.locations = new Locations(List.copyOf(locations));
    }

    /**
     * Sets the encoding of SQL migrations.
     */
    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets the encoding of SQL migrations.
     */
    public void setEncodingAsString(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    /**
     * Sets the default schema managed by MigrateDB. This schema name is case-sensitive. If not specified, but
     * <i>Schemas</i> is, MigrateDB uses the first schema in that list. If that is also not specified, MigrateDb uses
     * the default schema for the database connection.
     * <p>Consequences:</p>
     * <ul>
     * <li>This schema will be the one containing the schema history table.</li>
     * <li>This schema will be the default for the database connection (provided the database supports this concept)
     * .</li>
     * </ul>
     */
    public void setDefaultSchema(String schema) {
        this.defaultSchemaName = schema;
    }

    /**
     * Sets the schemas managed by MigrateDB. These schema names are case-sensitive. If not specified, MigrateDB uses
     * the default schema for the database connection. If <i>defaultSchema</i> is not specified, then the first of this
     * list also acts as default schema.
     * <p>Consequences:</p>
     * <ul>
     * <li>MigrateDB will automatically attempt to create all these schemas, unless they already exist.</li>
     * </ul>
     */
    public void setSchemas(String... schemas) {
        setSchemas(Arrays.asList(schemas));
    }

    /**
     * Sets the schemas managed by MigrateDB. These schema names are case-sensitive. If not specified, MigrateDB uses
     * the default schema for the database connection. If <i>defaultSchema</i> is not specified, then the first of this
     * list also acts as default schema.
     * <p>Consequences:</p>
     * <ul>
     * <li>MigrateDB will automatically attempt to create all these schemas, unless they already exist.</li>
     * </ul>            s
     */
    public void setSchemas(Collection<String> schemas) {
        this.schemas = List.copyOf(schemas);
    }

    /**
     * Sets the name of the schema history table that will be used by MigrateDB. By default (single-schema mode) the
     * schema history table is placed in the default schema for the connection provided by the datasource. When the
     * <i>migratedb.schemas</i> property is set (multi-schema mode), the schema history table is placed in the first
     * schema of the list.
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Sets the name of the old table to convert into the format used by MigrateDB. Only used for the "liberate"
     * command.
     */
    public void setOldTable(String oldTable) {
        this.oldTable = oldTable;
    }

    /**
     * Sets the tablespace where to create the schema history table that will be used by MigrateDB. If not specified,
     * MigrateDb uses the default tablespace for the database connection.This setting is only relevant for databases
     * that do support the notion of tablespaces. Its value is simply ignored for all others.
     */
    public void setTablespace(@Nullable String tablespace) {
        this.tablespace = tablespace;
    }

    /**
     * Sets the target version up to which MigrateDB should consider migrations. Migrations with a higher version number
     * will be ignored. Special values:
     * <ul>
     * <li>{@code current}: Designates the current version of the schema</li>
     * <li>{@code latest}: The latest version of the schema, as defined by the migration with the highest version</li>
     * <li>{@code next}: The next version of the schema, as defined by the first pending migration</li>
     * </ul>
     * Defaults to {@code latest}.
     */
    public void setTarget(TargetVersion target) {
        this.target = target;
    }

    /**
     * Sets the target version up to which MigrateDB should consider migrations. Migrations with a higher version number
     * will be ignored. Special values:
     * <ul>
     * <li>{@code current}: Designates the current version of the schema</li>
     * <li>{@code latest}: The latest version of the schema, as defined by the migration with the highest version</li>
     * <li>{@code next}: The next version of the schema, as defined by the first pending migration</li>
     * <li>
     *     &lt;version&gt;? (end with a '?'): Instructs MigrateDB not to fail if the target version doesn't exist.
     *     In this case, MigrateDb will go up to but not beyond the specified target
     *     (default: fail if the target version doesn't exist)
     * </li>
     * </ul>
     * Defaults to {@code latest}.
     */
    public void setTargetAsString(String target) {
        if (target.endsWith("?")) {
            setFailOnMissingTarget(false);
            setTarget(TargetVersion.parse(target.substring(0, target.length() - 1)));
        } else {
            setFailOnMissingTarget(true);
            setTarget(TargetVersion.parse(target));
        }
    }

    /**
     * Whether to fail if no migration with the configured target version exists (default: {@code true})
     */
    public void setFailOnMissingTarget(boolean failOnMissingTarget) {
        this.failOnMissingTarget = failOnMissingTarget;
    }

    /**
     * Sets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored.
     */
    public void setCherryPick(MigrationPattern... cherryPick) {
        setCherryPick(Arrays.asList(cherryPick));
    }

    /**
     * Sets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored.
     */
    public void setCherryPick(Collection<MigrationPattern> cherryPick) {
        this.cherryPick = List.copyOf(cherryPick);
    }

    /**
     * Sets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored. Values should be the version for versioned migrations
     * (e.g. 1, 2.4, 6.5.3) or the description for repeatable migrations (e.g. Insert_Data, Create_Table)
     */
    public void setCherryPickAsString(String... cherryPickAsString) {
        setCherryPickAsString(Arrays.asList(cherryPickAsString));
    }

    /**
     * Sets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored. Values should be the version for versioned migrations
     * (e.g. 1, 2.4, 6.5.3) or the description for repeatable migrations (e.g. Insert_Data, Create_Table)
     */
    public void setCherryPickAsString(Collection<String> cherryPickAsString) {
        this.cherryPick = cherryPickAsString.stream()
                                            .map(MigrationPattern::new)
                                            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Sets whether placeholders should be replaced.
     */
    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    /**
     * Sets the placeholders to replace in SQL migration scripts.
     */
    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Sets the prefix of every placeholder.
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        if (!StringUtils.hasLength(placeholderPrefix)) {
            throw new MigrateDbException("placeholderPrefix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Sets the prefix of every script placeholder.
     */
    public void setScriptPlaceholderPrefix(String scriptPlaceholderPrefix) {
        if (!StringUtils.hasLength(scriptPlaceholderPrefix)) {
            throw new MigrateDbException("scriptPlaceholderPrefix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.scriptPlaceholderPrefix = scriptPlaceholderPrefix;
    }

    /**
     * Sets the suffix of every placeholder.
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        if (!StringUtils.hasLength(placeholderSuffix)) {
            throw new MigrateDbException("placeholderSuffix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Sets the suffix of every placeholder.
     */
    public void setScriptPlaceholderSuffix(String scriptPlaceholderSuffix) {
        if (!StringUtils.hasLength(scriptPlaceholderSuffix)) {
            throw new MigrateDbException("scriptPlaceholderSuffix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.scriptPlaceholderSuffix = scriptPlaceholderSuffix;
    }

    /**
     * Sets the file name prefix for SQL migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    /**
     * Sets the file name prefix for baseline migrations. They have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to SB.1__My_description.sql
     */
    public void setBaselineMigrationPrefix(String baselineMigrationPrefix) {
        this.baselineMigrationPrefix = baselineMigrationPrefix;
    }

    /**
     * The additional Java-based migrations. These are not Java-based migrations discovered through classpath scanning
     * and instantiated by MigrateDB. Instead, these are application-managed instances of JavaMigration. This is
     * particularly useful when working with a dependency injection container, where you may want the DI container to
     * instantiate the class and wire up its dependencies for you.
     */
    public void setJavaMigrations(JavaMigration... javaMigrations) {
        this.javaMigrations = List.copyOf(Arrays.asList(javaMigrations));
    }

    /**
     * The additional Java-based migrations. These are not Java-based migrations discovered through classpath scanning
     * and instantiated by MigrateDB. Instead, these are application-managed instances of JavaMigration. This is
     * particularly useful when working with a dependency injection container, where you may want the DI container to
     * instantiate the class and wire up its dependencies for you.
     */
    public void setJavaMigrations(Collection<JavaMigration> javaMigrations) {
        this.javaMigrations = List.copyOf(javaMigrations);
    }

    /**
     * Sets the file name prefix for repeatable SQL migrations. Repeatable SQL migrations have the following file name
     * structure: prefixSeparatorDESCRIPTIONsuffix, which using the defaults translates to R__My_description.sql
     */
    public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
        this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
    }

    /**
     * Sets the file name separator for SQL migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     */
    public void setSqlMigrationSeparator(String sqlMigrationSeparator) {
        if (!StringUtils.hasLength(sqlMigrationSeparator)) {
            throw new MigrateDbException("sqlMigrationSeparator cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.sqlMigrationSeparator = sqlMigrationSeparator;
    }

    /**
     * The file name suffixes for SQL migrations. (default: .sql) SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql Multiple
     * suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
     * specific file associations.
     */
    public void setSqlMigrationSuffixes(String... sqlMigrationSuffixes) {
        setSqlMigrationSuffixes(Arrays.asList(sqlMigrationSuffixes));
    }

    /**
     * The file name suffixes for SQL migrations. (default: .sql) SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql Multiple
     * suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
     * specific file associations.
     */
    public void setSqlMigrationSuffixes(Collection<String> sqlMigrationSuffixes) {
        this.sqlMigrationSuffixes = List.copyOf(sqlMigrationSuffixes);
    }

    /**
     * Sets the data source to use. Must have the necessary privileges to execute DDL.
     */
    public void setDataSource(@Nullable DataSource dataSource) {
        this.dataSource = dataSource == null ? null : new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                return dataSource.getConnection();
            }

            @Override
            public String toString() {
                return "(" + dataSource + ")::getConnection";
            }
        };
    }

    /**
     * Sets the data source to use. Must have the necessary privileges to execute DDL.
     */
    public void setDataSource(@Nullable ConnectionProvider dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, MigrateDB
     * will wait 1 second before attempting to connect again, up to the maximum number of times specified by
     * connectRetries. The interval between retries doubles with each subsequent attempt.
     */
    public void setConnectRetries(int connectRetries) {
        if (connectRetries < 0) {
            throw new MigrateDbException("Invalid number of connectRetries (must be 0 or greater): " + connectRetries,
                                         ErrorCode.CONFIGURATION);
        }
        this.connectRetries = connectRetries;
    }

    /**
     * The maximum time between retries when attempting to connect to the database in seconds. This will cap the
     * interval between connect retry to the value provided.
     */
    public void setConnectRetriesInterval(int connectRetriesInterval) {
        if (connectRetriesInterval < 0) {
            throw new MigrateDbException(
                    "Invalid number for connectRetriesInterval (must be 0 or greater): " + connectRetriesInterval,
                    ErrorCode.CONFIGURATION);
        }
        this.connectRetriesInterval = connectRetriesInterval;
    }

    /**
     * The SQL statements to run to initialize a new database connection immediately after opening it.
     */
    public void setInitSql(String initSql) {
        this.initSql = initSql;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     */
    public void setBaselineVersion(Version baselineVersion) {
        this.baselineVersion = baselineVersion;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     */
    public void setBaselineVersionAsString(String baselineVersion) {
        this.baselineVersion = Version.parse(baselineVersion);
    }

    /**
     * Sets the description to tag an existing schema with when executing baseline.
     */
    public void setBaselineDescription(String baselineDescription) {
        this.baselineDescription = baselineDescription;
    }

    /**
     * Whether to automatically call baseline when migrate is executed against a non-empty schema with no schema history
     * table. This schema will then be baselined with the {@code baselineVersion} before executing the migrations. Only
     * migrations above {@code baselineVersion} will then be applied.
     * <p>
     * This is useful for initial MigrateDB production deployments on projects with an existing DB.
     * <p>
     * Be careful when enabling this as it removes the safety net that ensures MigrateDB does not migrate the wrong
     * database in case of a configuration mistake!
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    /**
     * Allows migrations to be run "out of order". If you already have versions 1 and 3 applied, and now a version 2 is
     * found, it will be applied too instead of being ignored.
     */
    public void setOutOfOrder(boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
    }

    /**
     * Whether MigrateDB should skip actually executing the contents of the migrations and only update the schema
     * history table. This should be used when you have applied a migration manually (via executing the sql yourself, or
     * via an IDE), and just want the schema history table to reflect this.
     * <p>
     * Use in conjunction with {@code cherryPick} to skip specific migrations instead of all pending ones.
     */
    public void setSkipExecutingMigrations(boolean skipExecutingMigrations) {
        this.skipExecutingMigrations = skipExecutingMigrations;
    }

    /**
     * Set the callbacks for lifecycle notifications.
     */
    public void setCallbacks(Callback... callbacks) {
        setCallbacks(Arrays.asList(callbacks));
    }

    /**
     * Set the callbacks for lifecycle notifications.
     */
    public void setCallbacks(Collection<Callback> callbacks) {
        this.callbacks = List.copyOf(callbacks);
    }

    /**
     * Set the callbacks for lifecycle notifications.
     */
    public void setCallbacksAsClassNames(String... callbacks) {
        setCallbacksAsClassNames(Arrays.asList(callbacks));
    }

    /**
     * Set the callbacks for lifecycle notifications.
     */
    public void setCallbacksAsClassNames(Collection<String> callbacks) {
        setCallbacks(ClassUtils.instantiateAll(callbacks, classLoader));
    }

    /**
     * Enables a single MigrateDB extension.
     */
    public void useExtension(MigrateDbExtension extension) {
        if (loadedExtensions.add(extension)) {
            databaseTypeRegister.registerDatabaseTypes(extension.getDatabaseTypes());
        }
    }

    /**
     * Enables multiple MigrateDB extensions. This is mainly used to load extensions from the {@code ServiceLoader}
     * facility:
     * <pre>{@code
     *      config.useExtensions(ServiceLoader.load(MigrateDbExtension.class))
     * }</pre>
     */
    public void useExtensions(Iterable<MigrateDbExtension> extensions) {
        for (var extension : extensions) {
            useExtension(extension);
        }
    }

    /**
     * Whether MigrateDB should skip the default callbacks. If true, only custom callbacks are used.
     */
    public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
        this.skipDefaultCallbacks = skipDefaultCallbacks;
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     */
    public void setResolvers(MigrationResolver... resolvers) {
        setResolvers(Arrays.asList(resolvers));
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     */
    public void setResolvers(Collection<MigrationResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     */
    public void setResolversAsClassNames(String... resolvers) {
        setResolversAsClassNames(Arrays.asList(resolvers));
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     */
    public void setResolversAsClassNames(Collection<String> resolvers) {
        setResolvers(ClassUtils.instantiateAll(resolvers, classLoader));
    }

    /**
     * Whether MigrateDB should skip the default resolvers. If true, only custom resolvers are used.
     */
    public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
        this.skipDefaultResolvers = skipDefaultResolvers;
    }

    /**
     * Whether MigrateDB should attempt to create the schemas specified in the schemas property.
     */
    public void setCreateSchemas(boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

    /**
     * Whether MigrateDB should output a table with the results of queries when executing migrations.
     */
    public void setOutputQueryResults(boolean outputQueryResults) {
        this.outputQueryResults = outputQueryResults;
    }

    /**
     * Set the custom ResourceProvider to be used to look up resources. If not set, the default strategy will be * used.
     * (default: null)
     */
    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    /**
     * The custom ClassProvider to be used to look up {@link JavaMigration} classes. If not set, the default * strategy
     * will be used. (default: null)
     */
    public void setJavaMigrationClassProvider(ClassProvider<JavaMigration> javaMigrationClassProvider) {
        this.javaMigrationClassProvider = javaMigrationClassProvider;
    }

    /**
     * Sets the maximum number of retries when trying to obtain a lock. -1 indicates attempting to repeat indefinitely.
     */
    public void setLockRetryCount(int lockRetryCount) {
        this.lockRetryCount = lockRetryCount;
    }

    /**
     * Whether to fail if a location specified in the {@code migratedb.locations} option doesn't exist
     */
    public void setFailOnMissingLocations(boolean failOnMissingLocations) {
        this.failOnMissingLocations = failOnMissingLocations;
    }

    /**
     * Whether the {@code liberate} command is automatically executed on {@code migrate} if the schema history table
     * does not exist, but {@code oldTable} exists. (Default: {@code true})
     */
    public void setLiberateOnMigrate(boolean liberateOnMigrate) {
        this.liberateOnMigrate = liberateOnMigrate;
    }

    /**
     * Sets the extension config of type {@code T}.
     */
    public <T extends ExtensionConfig> void setExtensionConfig(Class<T> extensionConfigType, T value) {
        extensionConfig.put(extensionConfigType, value);
    }

    /**
     * Configure with the same values as this existing configuration.
     * <p>To use a custom ClassLoader, it must be passed to the constructor prior to calling this method.</p>
     */
    @SuppressWarnings("OverlyLongMethod")
    public void configure(Configuration configuration) {
        extensionConfig.clear();
        extensionConfig.putAll(configuration.getExtensionConfig());
        loadedExtensions.clear();
        loadedExtensions.addAll(configuration.getLoadedExtensions());
        databaseTypeRegister.clear();
        databaseTypeRegister.registerDatabaseTypes(configuration.getDatabaseTypeRegister().getDatabaseTypes());

        setBaselineDescription(configuration.getBaselineDescription());
        setBaselineMigrationPrefix(configuration.getBaselineMigrationPrefix());
        setBaselineOnMigrate(configuration.isBaselineOnMigrate());
        setBaselineVersion(configuration.getBaselineVersion());
        setCallbacks(configuration.getCallbacks());
        setCherryPick(configuration.getCherryPick());
        setConnectRetries(configuration.getConnectRetries());
        setConnectRetriesInterval(configuration.getConnectRetriesInterval());
        setDataSource(configuration.getDataSource());
        setDefaultSchema(configuration.getDefaultSchema());
        setEncoding(configuration.getEncoding());
        setFailOnMissingLocations(configuration.isFailOnMissingLocations());
        setFailOnMissingTarget(configuration.isFailOnMissingTarget());
        setGroup(configuration.isGroup());
        setIgnoreFutureMigrations(configuration.isIgnoreFutureMigrations());
        setIgnoreIgnoredMigrations(configuration.isIgnoreIgnoredMigrations());
        setIgnoreMigrationPatterns(configuration.getIgnoreMigrationPatterns());
        setIgnoreMissingMigrations(configuration.isIgnoreMissingMigrations());
        setIgnorePendingMigrations(configuration.isIgnorePendingMigrations());
        setInitSql(configuration.getInitSql());
        setInstalledBy(configuration.getInstalledBy());
        setJavaMigrationClassProvider(configuration.getJavaMigrationClassProvider());
        setJavaMigrations(configuration.getJavaMigrations());
        setLocations(configuration.getLocations());
        setLockRetryCount(configuration.getLockRetryCount());
        setLogger(configuration.getLogger());
        setMixed(configuration.isMixed());
        setOldTable(configuration.getOldTable());
        setLiberateOnMigrate(configuration.isLiberateOnMigrate());
        setOutOfOrder(configuration.isOutOfOrder());
        setOutputQueryResults(configuration.isOutputQueryResults());
        setPlaceholderPrefix(configuration.getPlaceholderPrefix());
        setPlaceholderReplacement(configuration.isPlaceholderReplacement());
        setPlaceholders(configuration.getPlaceholders());
        setPlaceholderSuffix(configuration.getPlaceholderSuffix());
        setRepeatableSqlMigrationPrefix(configuration.getRepeatableSqlMigrationPrefix());
        setResolvers(configuration.getResolvers());
        setResourceProvider(configuration.getResourceProvider());
        setSchemas(configuration.getSchemas());
        setScriptPlaceholderPrefix(configuration.getScriptPlaceholderPrefix());
        setScriptPlaceholderSuffix(configuration.getScriptPlaceholderSuffix());
        setCreateSchemas(configuration.isCreateSchemas());
        setSkipDefaultCallbacks(configuration.isSkipDefaultCallbacks());
        setSkipDefaultResolvers(configuration.isSkipDefaultResolvers());
        setSkipExecutingMigrations(configuration.isSkipExecutingMigrations());
        setSqlMigrationPrefix(configuration.getSqlMigrationPrefix());
        setSqlMigrationSeparator(configuration.getSqlMigrationSeparator());
        setSqlMigrationSuffixes(configuration.getSqlMigrationSuffixes());
        setTable(configuration.getTable());
        setTablespace(configuration.getTablespace());
        setTarget(configuration.getTarget());
        setValidateMigrationNaming(configuration.isValidateMigrationNaming());
        setValidateOnMigrate(configuration.isValidateOnMigrate());
    }

    /**
     * Configures MigrateDB with these properties. This overwrites any existing configuration. Property names are
     * documented in {@link PropertyNames}. To use a custom ClassLoader, it must be passed to the MigrateDB constructor
     * prior to calling this method. To support the configuration of extensions, those extensions must be activated via
     * {@code useExtension} prior to calling this method.
     *
     * @param properties Properties used for configuration.
     * @throws MigrateDbException if the configuration fails.
     */
    public void configure(Properties properties) {
        configure(ConfigUtils.propertiesToMap(properties));
    }

    /**
     * Configures MigrateDB with these properties. This overwrites any existing configuration. Property names are
     * documented in {@link PropertyNames}. To use a custom ClassLoader, it must be passed to the MigrateDB constructor
     * prior to calling this method. To support the configuration of extensions, those extensions must be activated via
     * {@code useExtension} prior to calling this method.
     *
     * @param props Properties used for configuration.
     * @throws MigrateDbException if the configuration fails.
     */
    @SuppressWarnings("OverlyLongMethod")
    public void configure(Map<String, String> props) {
        // Make copy to prevent removing elements from the original.
        props = new HashMap<>(props);

        Integer connectRetriesProp = ConfigUtils.removeInteger(props, PropertyNames.CONNECT_RETRIES);
        if (connectRetriesProp != null) {
            setConnectRetries(connectRetriesProp);
        }
        Integer connectRetriesIntervalProp = ConfigUtils.removeInteger(props, PropertyNames.CONNECT_RETRIES_INTERVAL);
        if (connectRetriesIntervalProp != null) {
            setConnectRetriesInterval(connectRetriesIntervalProp);
        }
        String initSqlProp = props.remove(PropertyNames.INIT_SQL);
        if (initSqlProp != null) {
            setInitSql(initSqlProp);
        }
        String locationsProp = props.remove(PropertyNames.LOCATIONS);
        if (locationsProp != null) {
            setLocationsAsStrings(StringUtils.tokenizeToStringArray(locationsProp, ","));
        }
        Boolean placeholderReplacementProp = ConfigUtils.removeBoolean(props, PropertyNames.PLACEHOLDER_REPLACEMENT);
        if (placeholderReplacementProp != null) {
            setPlaceholderReplacement(placeholderReplacementProp);
        }
        String placeholderPrefixProp = props.remove(PropertyNames.PLACEHOLDER_PREFIX);
        if (placeholderPrefixProp != null) {
            setPlaceholderPrefix(placeholderPrefixProp);
        }
        String placeholderSuffixProp = props.remove(PropertyNames.PLACEHOLDER_SUFFIX);
        if (placeholderSuffixProp != null) {
            setPlaceholderSuffix(placeholderSuffixProp);
        }
        String scriptPlaceholderPrefixProp = props.remove(PropertyNames.SCRIPT_PLACEHOLDER_PREFIX);
        if (scriptPlaceholderPrefixProp != null) {
            setScriptPlaceholderPrefix(scriptPlaceholderPrefixProp);
        }
        String scriptPlaceholderSuffixProp = props.remove(PropertyNames.SCRIPT_PLACEHOLDER_SUFFIX);
        if (scriptPlaceholderSuffixProp != null) {
            setScriptPlaceholderSuffix(scriptPlaceholderSuffixProp);
        }
        String sqlMigrationPrefixProp = props.remove(PropertyNames.SQL_MIGRATION_PREFIX);
        if (sqlMigrationPrefixProp != null) {
            setSqlMigrationPrefix(sqlMigrationPrefixProp);
        }
        String baselineMigrationPrefixProp = props.remove(PropertyNames.BASELINE_MIGRATION_PREFIX);
        if (baselineMigrationPrefixProp != null) {
            setBaselineMigrationPrefix(baselineMigrationPrefixProp);
        }
        String repeatableSqlMigrationPrefixProp = props.remove(PropertyNames.REPEATABLE_SQL_MIGRATION_PREFIX);
        if (repeatableSqlMigrationPrefixProp != null) {
            setRepeatableSqlMigrationPrefix(repeatableSqlMigrationPrefixProp);
        }
        String sqlMigrationSeparatorProp = props.remove(PropertyNames.SQL_MIGRATION_SEPARATOR);
        if (sqlMigrationSeparatorProp != null) {
            setSqlMigrationSeparator(sqlMigrationSeparatorProp);
        }
        String sqlMigrationSuffixesProp = props.remove(PropertyNames.SQL_MIGRATION_SUFFIXES);
        if (sqlMigrationSuffixesProp != null) {
            setSqlMigrationSuffixes(StringUtils.tokenizeToStringArray(sqlMigrationSuffixesProp, ","));
        }
        String encodingProp = props.remove(PropertyNames.ENCODING);
        if (encodingProp != null) {
            setEncodingAsString(encodingProp);
        }
        String defaultSchemaProp = props.remove(PropertyNames.DEFAULT_SCHEMA);
        if (defaultSchemaProp != null) {
            setDefaultSchema(defaultSchemaProp);
        }
        String schemasProp = props.remove(PropertyNames.SCHEMAS);
        if (schemasProp != null) {
            setSchemas(StringUtils.tokenizeToStringArray(schemasProp, ","));
        }
        String tableProp = props.remove(PropertyNames.TABLE);
        if (tableProp != null) {
            setTable(tableProp);
        }
        String oldTableProp = props.remove(PropertyNames.OLD_TABLE);
        if (oldTableProp != null) {
            setOldTable(oldTableProp);
        }
        Boolean liberateOnMigrateProp = ConfigUtils.removeBoolean(props, PropertyNames.LIBERATE_ON_MIGRATE);
        if (liberateOnMigrateProp != null) {
            setLiberateOnMigrate(liberateOnMigrateProp);
        }
        String tablespaceProp = props.remove(PropertyNames.TABLESPACE);
        if (tablespaceProp != null) {
            setTablespace(tablespaceProp);
        }
        Boolean validateOnMigrateProp = ConfigUtils.removeBoolean(props, PropertyNames.VALIDATE_ON_MIGRATE);
        if (validateOnMigrateProp != null) {
            setValidateOnMigrate(validateOnMigrateProp);
        }
        String baselineVersionProp = props.remove(PropertyNames.BASELINE_VERSION);
        if (baselineVersionProp != null) {
            setBaselineVersionAsString(baselineVersionProp);
        }
        String baselineDescriptionProp = props.remove(PropertyNames.BASELINE_DESCRIPTION);
        if (baselineDescriptionProp != null) {
            setBaselineDescription(baselineDescriptionProp);
        }
        Boolean baselineOnMigrateProp = ConfigUtils.removeBoolean(props, PropertyNames.BASELINE_ON_MIGRATE);
        if (baselineOnMigrateProp != null) {
            setBaselineOnMigrate(baselineOnMigrateProp);
        }
        Boolean ignoreMissingMigrationsProp = ConfigUtils.removeBoolean(props, PropertyNames.IGNORE_MISSING_MIGRATIONS);
        if (ignoreMissingMigrationsProp != null) {
            setIgnoreMissingMigrations(ignoreMissingMigrationsProp);
        }
        Boolean ignoreIgnoredMigrationsProp = ConfigUtils.removeBoolean(props, PropertyNames.IGNORE_IGNORED_MIGRATIONS);
        if (ignoreIgnoredMigrationsProp != null) {
            setIgnoreIgnoredMigrations(ignoreIgnoredMigrationsProp);
        }
        Boolean ignorePendingMigrationsProp = ConfigUtils.removeBoolean(props, PropertyNames.IGNORE_PENDING_MIGRATIONS);
        if (ignorePendingMigrationsProp != null) {
            setIgnorePendingMigrations(ignorePendingMigrationsProp);
        }
        Boolean ignoreFutureMigrationsProp = ConfigUtils.removeBoolean(props, PropertyNames.IGNORE_FUTURE_MIGRATIONS);
        if (ignoreFutureMigrationsProp != null) {
            setIgnoreFutureMigrations(ignoreFutureMigrationsProp);
        }
        Boolean validateMigrationNamingProp = ConfigUtils.removeBoolean(props, PropertyNames.VALIDATE_MIGRATION_NAMING);
        if (validateMigrationNamingProp != null) {
            setValidateMigrationNaming(validateMigrationNamingProp);
        }
        String targetProp = props.remove(PropertyNames.TARGET);
        if (targetProp != null) {
            setTargetAsString(targetProp);
        }
        String cherryPickProp = props.remove(PropertyNames.CHERRY_PICK);
        if (cherryPickProp != null) {
            setCherryPickAsString(StringUtils.tokenizeToStringArray(cherryPickProp, ","));
        }
        String loggersProp = props.remove(PropertyNames.LOGGER);
        if (loggersProp != null) {
            setLogger(StringUtils.tokenizeToStringArray(loggersProp, ","));
        }
        Integer lockRetryCount = ConfigUtils.removeInteger(props, PropertyNames.LOCK_RETRY_COUNT);
        if (lockRetryCount != null) {
            setLockRetryCount(lockRetryCount);
        }
        Boolean outOfOrderProp = ConfigUtils.removeBoolean(props, PropertyNames.OUT_OF_ORDER);
        if (outOfOrderProp != null) {
            setOutOfOrder(outOfOrderProp);
        }
        Boolean skipExecutingMigrationsProp = ConfigUtils.removeBoolean(props, PropertyNames.SKIP_EXECUTING_MIGRATIONS);
        if (skipExecutingMigrationsProp != null) {
            setSkipExecutingMigrations(skipExecutingMigrationsProp);
        }
        Boolean outputQueryResultsProp = ConfigUtils.removeBoolean(props, PropertyNames.OUTPUT_QUERY_RESULTS);
        if (outputQueryResultsProp != null) {
            setOutputQueryResults(outputQueryResultsProp);
        }
        String resolversProp = props.remove(PropertyNames.RESOLVERS);
        if (StringUtils.hasLength(resolversProp)) {
            setResolversAsClassNames(StringUtils.tokenizeToStringArray(resolversProp, ","));
        }
        Boolean skipDefaultResolversProp = ConfigUtils.removeBoolean(props, PropertyNames.SKIP_DEFAULT_RESOLVERS);
        if (skipDefaultResolversProp != null) {
            setSkipDefaultResolvers(skipDefaultResolversProp);
        }
        String callbacksProp = props.remove(PropertyNames.CALLBACKS);
        if (StringUtils.hasLength(callbacksProp)) {
            setCallbacksAsClassNames(StringUtils.tokenizeToStringArray(callbacksProp, ","));
        }
        Boolean skipDefaultCallbacksProp = ConfigUtils.removeBoolean(props, PropertyNames.SKIP_DEFAULT_CALLBACKS);
        if (skipDefaultCallbacksProp != null) {
            setSkipDefaultCallbacks(skipDefaultCallbacksProp);
        }
        putPropertiesUnderNamespace(props, placeholders, PropertyNames.PLACEHOLDERS_PROPERTY_PREFIX);
        Boolean mixedProp = ConfigUtils.removeBoolean(props, PropertyNames.MIXED);
        if (mixedProp != null) {
            setMixed(mixedProp);
        }
        Boolean groupProp = ConfigUtils.removeBoolean(props, PropertyNames.GROUP);
        if (groupProp != null) {
            setGroup(groupProp);
        }
        String installedByProp = props.remove(PropertyNames.INSTALLED_BY);
        if (installedByProp != null) {
            setInstalledBy(installedByProp);
        }
        Boolean createSchemasProp = ConfigUtils.removeBoolean(props, PropertyNames.CREATE_SCHEMAS);
        if (createSchemasProp != null) {
            setCreateSchemas(createSchemasProp);
        }
        String ignoreMigrationPatternsProp = props.remove(PropertyNames.IGNORE_MIGRATION_PATTERNS);
        if (ignoreMigrationPatternsProp != null) {
            setIgnoreMigrationPatternsAsStrings(StringUtils.tokenizeToStringArray(ignoreMigrationPatternsProp, ","));
        }
        Boolean failOnMissingLocationsProp = ConfigUtils.removeBoolean(props, PropertyNames.FAIL_ON_MISSING_LOCATIONS);
        if (failOnMissingLocationsProp != null) {
            setFailOnMissingLocations(failOnMissingLocationsProp);
        }

        for (var extension : loadedExtensions) {
            for (var converter : extension.getConfigPropertiesConverters()) {
                var result = converter.convert(props);
                extensionConfig.put(result.extensionConfigType, result.config);
            }
        }

        ConfigUtils.reportUnrecognisedProperties(props, "migratedb.");
    }

    private void putPropertiesUnderNamespace(Map<String, String> properties, Map<String, String> target,
                                             String namespace) {
        var iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String propertyName = entry.getKey();

            if (propertyName.startsWith(namespace) && propertyName.length() > namespace.length()) {
                String placeholderName = propertyName.substring(namespace.length());
                String placeholderValue = entry.getValue();
                target.put(placeholderName, placeholderValue);
                iterator.remove();
            }
        }
    }
}
