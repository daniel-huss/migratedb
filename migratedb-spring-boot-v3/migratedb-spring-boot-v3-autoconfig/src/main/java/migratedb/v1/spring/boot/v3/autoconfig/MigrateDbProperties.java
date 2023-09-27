/*
 * Copyright 2012-2023 the original author or authors.
 * Copyright 2023 The MigrateDB contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.api.ExtensionConfig;
import migratedb.v1.core.api.pattern.ValidatePattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for MigrateDB database migrations.
 *
 * @author Daniel Huss
 */
@ConfigurationProperties(prefix = "migratedb")
public class MigrateDbProperties {
    /**
     * Whether to enable MigrateDB.
     */
    private boolean enabled = true;

    /**
     * Whether the autoconfiguration should also load extensions from the {@link java.util.ServiceLoader} facility.
     * Disabled by default, so only beans that implement {@link migratedb.v1.core.api.MigrateDbExtension} will be
     * loaded.
     */
    private boolean useServiceLoader = false;

    /**
     * If set, database migrations will use a data source that is derived from the application data source, but uses
     * different credentials.
     */
    private @Nullable String user;

    /**
     * If set, database migrations will use a data source that is derived from the application data source, but uses
     * different credentials.
     */
    private @Nullable String password;

    /**
     * If set, database migrations will use this data source instead of the application data source.
     */
    private @Nullable DataSourceProperties dataSource;

    /**
     * Extension-specific properties. In addition, Spring beans that implement {@link ExtensionConfig} are
     * auto-detected.
     */
    private @Nullable Map<String, String> extensionConfig;

    // Everything below this line is a copied from the MigrateDB configuration class

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, MigrateDB
     * will wait 1 second before attempting to connect again, up to the maximum number of times specified by
     * connectRetries. The interval between retries doubles with each subsequent attempt.
     */
    private @Nullable Integer connectRetries;

    /**
     * The maximum time between retries when attempting to connect to the database in seconds. This will cap the
     * interval between connect retry to the value provided.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectRetriesInterval;

    /**
     * The SQL statements to run to initialize a new database connection immediately after opening it.
     */
    private @Nullable String initSql;

    /**
     * The version to tag an existing schema with when executing baseline.
     */
    private @Nullable String baselineVersion;

    /**
     * The description to tag an existing schema with when executing baseline. (default: &lt;&lt; MigrateDB Baseline
     * &gt;&gt;)
     */
    private @Nullable String baselineDescription;

    /**
     * Whether default built-in callbacks should be skipped. If true, only custom callbacks are used. (default: false)
     */
    private @Nullable Boolean skipDefaultCallbacks;

    /**
     * Whether default built-in resolvers should be skipped. If true, only custom resolvers are used. (default: false)
     */
    private @Nullable Boolean skipDefaultResolvers;

    /**
     * The file name prefix for versioned SQL migrations. Versioned SQL migrations have the following file name
     * structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to
     * V1.1__My_description.sql (default: V)
     */
    private @Nullable String sqlMigrationPrefix;

    /**
     * The file name prefix for baseline migrations. Baseline migrations represent all migrations with
     * {@code version ≤ current baseline migration version} while keeping older migrations if needed for upgrading
     * older deployments. They have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which
     * using the defaults translates to B1.1__My_description.sql. (default: B)
     */
    private @Nullable String baselineMigrationPrefix;

    /**
     * The file name prefix for repeatable sql migrations. Repeatable SQL migrations have the following file name
     * structure: prefixSeparatorDESCRIPTIONsuffix, which using the defaults translates to R__My_description.sql.
     * (default: R)
     */
    private @Nullable String repeatableSqlMigrationPrefix;

    /**
     * The file name separator for sql migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql.
     * (default: __)
     */
    private @Nullable String sqlMigrationSeparator;

    /**
     * The file name suffixes for SQL migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql Multiple
     * suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
     * specific file associations. (default: .sql)
     */
    private @Nullable List<String> sqlMigrationSuffixes;

    /**
     * Whether placeholders should be replaced. (default: true)
     */
    private @Nullable Boolean placeholderReplacement;

    /**
     * The suffix of every placeholder. (default: } )
     */
    private @Nullable String placeholderSuffix;

    /**
     * The prefix of every placeholder. (default: ${ )
     */
    private @Nullable String placeholderPrefix;

    /**
     * The suffix of every script placeholder. (default: __ )
     */
    private @Nullable String scriptPlaceholderSuffix;

    /**
     * The prefix of every script placeholder. (default: FP__ )
     */
    private @Nullable String scriptPlaceholderPrefix;

    /**
     * The map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    private @Nullable Map<String, String> placeholders;

    /**
     * The target version up to which MigrateDB should consider migrations. Migrations with a higher version number will
     * be ignored. Special values:
     * <ul>
     * <li>{@code current}: Designates the current version of the schema</li>
     * <li>{@code latest}: The latest version of the schema, as defined by the migration with the highest version</li>
     * <li>
     *     &lt;version&gt;? (end with a '?'): Instructs MigrateDB not to fail if the target version doesn't exist.
     *     In this case, MigrateDB will go up to but not beyond the specified target
     *     (default: fail if the target version doesn't exist)
     * </li>
     * </ul> Defaults to {@code latest}
     */
    private @Nullable String target;

    /**
     * Whether to fail if no migration with the configured target version exists (default: {@code true})
     */
    private @Nullable Boolean failOnMissingTarget;

    /**
     * The migrations that MigrateDb should consider when migrating. Leave empty to consider all available migrations.
     * Migrations not in this list will be ignored.
     */
    private @Nullable List<String> cherryPick;

    /**
     * The name of the schema history table that will be used by MigrateDB. By default, (single-schema mode) the schema
     * history table is placed in the default schema for the connection provided by the datasource. When the
     * <i>migratedb.schemas</i> property is set (multi-schema mode), the schema history table is placed in the first
     * schema of the list. (default: migratedb_state)
     */
    private @Nullable String table;

    /**
     * The old table to convert into the format used by MigrateDB. Only used for the "liberate" command.
     */
    private @Nullable String oldTable;

    /**
     * Whether the {@code liberate} command is automatically executed on {@code migrate} if the schema history table
     * does not exist, but {@code oldTable} exists. (Default: {@code true})
     */
    private @Nullable Boolean liberateOnMigrate;

    /**
     * The tablespace where to create the schema history table that will be used by MigrateDB. If not specified,
     * MigrateDB uses the default tablespace for the database connection. This setting is only relevant for databases
     * that do support the notion of tablespaces. Its value is simply ignored for all others.
     */
    private @Nullable String tablespace;

    /**
     * The default schema managed by MigrateDB. This schema name is case-sensitive. If not specified, but
     * <i>schemas</i>
     * is, MigrateDB uses the first schema in that list. If that is also not specified, MigrateDb uses the default
     * schema for the database connection.
     * <p>Consequences:</p>
     * <ul>
     * <li>This schema will be the one containing the schema history table.</li>
     * <li>This schema will be the default for the database connection (provided the database supports this concept)
     * .</li>
     * </ul> (default: The first schema specified in getSchemas(), and failing that
     * the default schema for the database connection)
     */
    private @Nullable String defaultSchema;

    /**
     * The schemas managed by MigrateDB. These schema names are case-sensitive. If not specified, MigrateDB uses the
     * default schema for the database connection. If <i>defaultSchemaName</i> is not specified, then the first of this
     * list also acts as default schema.
     * <p>Consequences:</p>
     * <ul>
     * <li>MigrateDB will automatically attempt to create all these schemas, unless they already exist.</li>
     * </ul> (default: The default schema for the database connection)
     */
    private @Nullable List<String> schemas;

    /**
     * The encoding of SQL migrations. (default: UTF-8)
     */
    private @Nullable Charset encoding;

    /**
     * The locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     * (default: classpath:db/migration)
     */
    private @Nullable List<String> locations;

    /**
     * Whether to automatically call baseline when migrate is executed against a non-empty schema with no schema history
     * table. This schema will then be initialized with the {@code baselineVersion} before executing the migrations.
     * Only migrations above {@code baselineVersion} will then be applied.
     * <p>
     * This is useful for initial MigrateDB production deployments on projects with an existing DB.
     * <p>
     * Be careful when enabling this as it removes the safety net that ensures MigrateDB does not migrate the wrong
     * database in case of a configuration mistake! (default: {@code false})
     */
    private @Nullable Boolean baselineOnMigrate;

    /**
     * Whether MigrateDB should skip actually executing the contents of the migrations and only update the schema
     * history table. This should be used when you have applied a migration manually (via executing the sql yourself, or
     * via an ide), and just want the schema history table to reflect this.
     * <p>
     * Use in conjunction with {@code cherryPick} to skip specific migrations instead of all pending ones. (default:
     * {@code false})
     */
    private @Nullable Boolean skipExecutingMigrations;

    /**
     * Whether migrations are allowed to be run "out of order". If you already have versions 1 and 3 applied, and now a
     * version 2 is found, it will be applied too instead of being ignored. (default: {@code false})
     */
    private @Nullable Boolean outOfOrder;

    /**
     * Ignore missing migrations when reading the schema history table. These are migrations that were performed by an
     * older deployment of the application that are no longer available in this version. For example: we have migrations
     * available on the classpath with versions 1.0 and 3.0. The schema history table indicates that a migration with
     * version 2.0 (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDB continues normally. This is useful for situations where one must be able to
     * deploy a newer version of the application even though it doesn't contain migrations included with an older one
     * anymore. Note that if the most recently applied migration is removed, MigrateDb has no way to know it is missing
     * and will mark it as future instead. {@code true} to continue normally and log a warning, {@code false} to fail
     * fast with an exception. (default: {@code false})
     */
    private @Nullable Boolean ignoreMissingMigrations;

    /**
     * Ignore ignored migrations when reading the schema history table. These are migrations that were added in between
     * already migrated migrations in this version. For example: we have migrations available on the classpath with
     * versions from 1.0 to 3.0. The schema history table indicates that version 1 was finished on 1.0.15, and the next
     * one was 2.0.0. But with the next release a new migration was added to version 1: 1.0.16. Such scenario is ignored
     * by migrate command, but by default is rejected by validate. When ignoreIgnoredMigrations is enabled, such case
     * will not be reported by validate command. This is useful for situations where one must be able to deliver
     * complete set of migrations in a delivery package for multiple versions of the product, and allows for further
     * development of older versions. {@code true} to continue normally, {@code false} to fail fast with an exception.
     * (default: {@code false})
     */
    private @Nullable Boolean ignoreIgnoredMigrations;

    /**
     * Ignore pending migrations when reading the schema history table. These are migrations that are available but have
     * not yet been applied. This can be useful for verifying that in-development migration changes don't contain any
     * validation-breaking changes of migrations that have already been applied to a production environment, e.g. as
     * part of a CI/CD process, without failing because of the existence of new migration versions. {@code true} to
     * continue normally, {@code false} to fail fast with an exception. (default: {@code false})
     */
    private @Nullable Boolean ignorePendingMigrations;

    /**
     * Ignore future migrations when reading the schema history table. These are migrations that were performed by a
     * newer deployment of the application that are not yet available in this version. For example: we have migrations
     * available on the classpath up to version 3.0. The schema history table indicates that a migration to version 4.0
     * (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a warning is
     * logged and MigrateDB continues normally. This is useful for situations where one must be able to redeploy an
     * older version of the application after the database has been migrated by a newer one. {@code true} to continue
     * normally and log a warning, {@code false} to fail fast with an exception. (default: {@code true})
     */
    private @Nullable Boolean ignoreFutureMigrations;

    /**
     * Patterns of ignored migrations. Each pattern is of the form {@code <migration_type>:<migration_state>}. See  <a
     * href="https://daniel-huss.github.io/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns">the
     * website</a> for full details.
     * <p>Example: repeatable:missing,versioned:pending,*:failed</p>
     * <p>(default: none)</p>
     */
    private @Nullable List<ValidatePattern> ignoreMigrationPatterns;

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure
     * can be useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     * {@code false} to continue normally, {@code true} to fail fast with an exception. (default: {@code false})
     */
    private @Nullable Boolean validateMigrationNaming;

    /**
     * Whether to automatically call validate or not when running migrate. {@code true} if validate should be called.
     * {@code false} if not. (default: {@code true})
     */
    private @Nullable Boolean validateOnMigrate;

    /**
     * Whether to allow mixing transactional and non-transactional statements within the same migration. Enabling this
     * automatically causes the entire affected migration to be run without a transaction.
     * <p>
     * Note that this is only applicable for PostgreSQL, Aurora PostgreSQL, SQL Server and SQLite which all have
     * statements that do not run at all within a transaction. This is not to be confused with implicit transaction, as
     * they occur in MySQL or Oracle, where even though a DDL statement was run within a transaction, the database will
     * issue an implicit commit before and after its execution. {@code true} if mixed migrations should be allowed.
     * {@code false} if an error should be thrown instead. (default: {@code false})
     */
    private @Nullable Boolean mixed;

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for
     * databases with support for DDL transactions). {@code true} if migrations should be grouped. {@code false} if
     * they should be applied individually instead. (default: {@code false})
     */
    private @Nullable Boolean group;

    /**
     * The username that will be recorded in the schema history table as having applied the migration, or {@code null}
     * for the current database user of the connection (default: {@code null}).
     */
    private @Nullable String installedBy;

    /**
     * Whether MigrateDB should output a table with the results of queries when executing migrations. {@code true} to
     * output the results table (default: {@code true})
     */
    private @Nullable Boolean outputQueryResults;

    /**
     * Whether MigrateDB should attempt to create the schemas specified in the {@code schemas} property. (default:
     * {@code true})
     */
    private @Nullable Boolean createSchemas;

    /**
     * The maximum number of retries when trying to obtain a lock. -1 indicates attempting to repeat indefinitely.
     */
    private @Nullable Integer lockRetryCount;

    /**
     * Whether to fail if a location specified in the {@code migratedb.locations} option doesn't exist. (default:
     * {@code false})
     */
    private @Nullable Boolean failOnMissingLocations;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseServiceLoader() {
        return useServiceLoader;
    }

    public void setUseServiceLoader(boolean useServiceLoader) {
        this.useServiceLoader = useServiceLoader;
    }

    public @Nullable String getUser() {
        return user;
    }

    public void setUser(@Nullable String user) {
        this.user = user;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public @Nullable DataSourceProperties getDataSource() {
        return dataSource;
    }

    public void setDataSource(@Nullable DataSourceProperties dataSource) {
        this.dataSource = dataSource;
    }

    public @Nullable Map<String, String> getExtensionConfig() {
        return extensionConfig;
    }

    public void setExtensionConfig(@Nullable Map<String, String> extensionConfig) {
        this.extensionConfig = extensionConfig;
    }

    public @Nullable Integer getConnectRetries() {
        return connectRetries;
    }

    public void setConnectRetries(@Nullable Integer connectRetries) {
        this.connectRetries = connectRetries;
    }

    public Duration getConnectRetriesInterval() {
        return connectRetriesInterval;
    }

    public void setConnectRetriesInterval(Duration connectRetriesInterval) {
        this.connectRetriesInterval = connectRetriesInterval;
    }

    public @Nullable String getInitSql() {
        return initSql;
    }

    public void setInitSql(@Nullable String initSql) {
        this.initSql = initSql;
    }

    public @Nullable String getBaselineVersion() {
        return baselineVersion;
    }

    public void setBaselineVersion(@Nullable String baselineVersion) {
        this.baselineVersion = baselineVersion;
    }

    public @Nullable String getBaselineDescription() {
        return baselineDescription;
    }

    public void setBaselineDescription(@Nullable String baselineDescription) {
        this.baselineDescription = baselineDescription;
    }

    public @Nullable Boolean getSkipDefaultCallbacks() {
        return skipDefaultCallbacks;
    }

    public void setSkipDefaultCallbacks(@Nullable Boolean skipDefaultCallbacks) {
        this.skipDefaultCallbacks = skipDefaultCallbacks;
    }

    public @Nullable Boolean getSkipDefaultResolvers() {
        return skipDefaultResolvers;
    }

    public void setSkipDefaultResolvers(@Nullable Boolean skipDefaultResolvers) {
        this.skipDefaultResolvers = skipDefaultResolvers;
    }

    public @Nullable String getSqlMigrationPrefix() {
        return sqlMigrationPrefix;
    }

    public void setSqlMigrationPrefix(@Nullable String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    public @Nullable String getBaselineMigrationPrefix() {
        return baselineMigrationPrefix;
    }

    public void setBaselineMigrationPrefix(@Nullable String baselineMigrationPrefix) {
        this.baselineMigrationPrefix = baselineMigrationPrefix;
    }

    public @Nullable String getRepeatableSqlMigrationPrefix() {
        return repeatableSqlMigrationPrefix;
    }

    public void setRepeatableSqlMigrationPrefix(@Nullable String repeatableSqlMigrationPrefix) {
        this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
    }

    public @Nullable String getSqlMigrationSeparator() {
        return sqlMigrationSeparator;
    }

    public void setSqlMigrationSeparator(@Nullable String sqlMigrationSeparator) {
        this.sqlMigrationSeparator = sqlMigrationSeparator;
    }

    public @Nullable List<String> getSqlMigrationSuffixes() {
        return sqlMigrationSuffixes;
    }

    public void setSqlMigrationSuffixes(@Nullable List<String> sqlMigrationSuffixes) {
        this.sqlMigrationSuffixes = sqlMigrationSuffixes;
    }

    public @Nullable Boolean getPlaceholderReplacement() {
        return placeholderReplacement;
    }

    public void setPlaceholderReplacement(@Nullable Boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    public @Nullable String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(@Nullable String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public @Nullable String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(@Nullable String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public @Nullable String getScriptPlaceholderSuffix() {
        return scriptPlaceholderSuffix;
    }

    public void setScriptPlaceholderSuffix(@Nullable String scriptPlaceholderSuffix) {
        this.scriptPlaceholderSuffix = scriptPlaceholderSuffix;
    }

    public @Nullable String getScriptPlaceholderPrefix() {
        return scriptPlaceholderPrefix;
    }

    public void setScriptPlaceholderPrefix(@Nullable String scriptPlaceholderPrefix) {
        this.scriptPlaceholderPrefix = scriptPlaceholderPrefix;
    }

    public @Nullable Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(@Nullable Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public @Nullable String getTarget() {
        return target;
    }

    public void setTarget(@Nullable String target) {
        this.target = target;
    }

    public @Nullable Boolean getFailOnMissingTarget() {
        return failOnMissingTarget;
    }

    public void setFailOnMissingTarget(@Nullable Boolean failOnMissingTarget) {
        this.failOnMissingTarget = failOnMissingTarget;
    }

    public @Nullable List<String> getCherryPick() {
        return cherryPick;
    }

    public void setCherryPick(@Nullable List<String> cherryPick) {
        this.cherryPick = cherryPick;
    }

    public @Nullable String getTable() {
        return table;
    }

    public void setTable(@Nullable String table) {
        this.table = table;
    }

    public @Nullable String getOldTable() {
        return oldTable;
    }

    public void setOldTable(@Nullable String oldTable) {
        this.oldTable = oldTable;
    }

    public @Nullable Boolean getLiberateOnMigrate() {
        return liberateOnMigrate;
    }

    public void setLiberateOnMigrate(@Nullable Boolean liberateOnMigrate) {
        this.liberateOnMigrate = liberateOnMigrate;
    }

    public @Nullable String getTablespace() {
        return tablespace;
    }

    public void setTablespace(@Nullable String tablespace) {
        this.tablespace = tablespace;
    }

    public @Nullable String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(@Nullable String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public @Nullable List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(@Nullable List<String> schemas) {
        this.schemas = schemas;
    }

    public @Nullable Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(@Nullable Charset encoding) {
        this.encoding = encoding;
    }

    public @Nullable List<String> getLocations() {
        return locations;
    }

    public void setLocations(@Nullable List<String> locations) {
        this.locations = locations;
    }

    public @Nullable Boolean getBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    public void setBaselineOnMigrate(@Nullable Boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    public @Nullable Boolean getSkipExecutingMigrations() {
        return skipExecutingMigrations;
    }

    public void setSkipExecutingMigrations(@Nullable Boolean skipExecutingMigrations) {
        this.skipExecutingMigrations = skipExecutingMigrations;
    }

    public @Nullable Boolean getOutOfOrder() {
        return outOfOrder;
    }

    public void setOutOfOrder(@Nullable Boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
    }

    public @Nullable Boolean getIgnoreMissingMigrations() {
        return ignoreMissingMigrations;
    }

    public void setIgnoreMissingMigrations(@Nullable Boolean ignoreMissingMigrations) {
        this.ignoreMissingMigrations = ignoreMissingMigrations;
    }

    public @Nullable Boolean getIgnoreIgnoredMigrations() {
        return ignoreIgnoredMigrations;
    }

    public void setIgnoreIgnoredMigrations(@Nullable Boolean ignoreIgnoredMigrations) {
        this.ignoreIgnoredMigrations = ignoreIgnoredMigrations;
    }

    public @Nullable Boolean getIgnorePendingMigrations() {
        return ignorePendingMigrations;
    }

    public void setIgnorePendingMigrations(@Nullable Boolean ignorePendingMigrations) {
        this.ignorePendingMigrations = ignorePendingMigrations;
    }

    public @Nullable Boolean getIgnoreFutureMigrations() {
        return ignoreFutureMigrations;
    }

    public void setIgnoreFutureMigrations(@Nullable Boolean ignoreFutureMigrations) {
        this.ignoreFutureMigrations = ignoreFutureMigrations;
    }

    public @Nullable List<ValidatePattern> getIgnoreMigrationPatterns() {
        return ignoreMigrationPatterns;
    }

    public void setIgnoreMigrationPatterns(@Nullable List<ValidatePattern> ignoreMigrationPatterns) {
        this.ignoreMigrationPatterns = ignoreMigrationPatterns;
    }

    public @Nullable Boolean getValidateMigrationNaming() {
        return validateMigrationNaming;
    }

    public void setValidateMigrationNaming(@Nullable Boolean validateMigrationNaming) {
        this.validateMigrationNaming = validateMigrationNaming;
    }

    public @Nullable Boolean getValidateOnMigrate() {
        return validateOnMigrate;
    }

    public void setValidateOnMigrate(@Nullable Boolean validateOnMigrate) {
        this.validateOnMigrate = validateOnMigrate;
    }

    public @Nullable Boolean getMixed() {
        return mixed;
    }

    public void setMixed(@Nullable Boolean mixed) {
        this.mixed = mixed;
    }

    public @Nullable Boolean getGroup() {
        return group;
    }

    public void setGroup(@Nullable Boolean group) {
        this.group = group;
    }

    public @Nullable String getInstalledBy() {
        return installedBy;
    }

    public void setInstalledBy(@Nullable String installedBy) {
        this.installedBy = installedBy;
    }

    public @Nullable Boolean getOutputQueryResults() {
        return outputQueryResults;
    }

    public void setOutputQueryResults(@Nullable Boolean outputQueryResults) {
        this.outputQueryResults = outputQueryResults;
    }

    public @Nullable Boolean getCreateSchemas() {
        return createSchemas;
    }

    public void setCreateSchemas(@Nullable Boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

    public @Nullable Integer getLockRetryCount() {
        return lockRetryCount;
    }

    public void setLockRetryCount(@Nullable Integer lockRetryCount) {
        this.lockRetryCount = lockRetryCount;
    }

    public @Nullable Boolean getFailOnMissingLocations() {
        return failOnMissingLocations;
    }

    public void setFailOnMissingLocations(@Nullable Boolean failOnMissingLocations) {
        this.failOnMissingLocations = failOnMissingLocations;
    }
}
