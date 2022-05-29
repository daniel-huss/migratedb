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
package migratedb.core.api.configuration;

import migratedb.core.MigrateDb;
import migratedb.core.api.*;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.logging.LogSystem;
import migratedb.core.api.logging.LogSystems;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.pattern.ValidatePattern;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.util.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;

/**
 * Fluent configuration for MigrateDB . This is the preferred means of configuring the MigrateDB API. This configuration
 * can be passed to MigrateDB using the {@code new MigrateDb(Configuration)} constructor.
 */
public class FluentConfiguration implements Configuration {
    private final ClassicConfiguration config;

    public FluentConfiguration() {
        config = new ClassicConfiguration();
    }

    /**
     * @param classLoader The ClassLoader to use for loading migrations, resolvers, etc from the classpath. (default:
     *                    same as {@link ClassUtils#defaultClassLoader()} )
     */
    public FluentConfiguration(ClassLoader classLoader) {
        config = new ClassicConfiguration(classLoader);
    }

    /**
     * @return The new fully-configured MigrateDb instance.
     */
    public MigrateDb load() {
        return new MigrateDb(this);
    }

    /**
     * Configure with the same values as this existing configuration.
     */
    public FluentConfiguration configuration(Configuration configuration) {
        config.configure(configuration);
        return this;
    }

    @Override
    public Location[] getLocations() {
        return config.getLocations();
    }

    @Override
    public Charset getEncoding() {
        return config.getEncoding();
    }

    @Override
    public String getDefaultSchema() {
        return config.getDefaultSchema();
    }

    @Override
    public String[] getSchemas() {
        return config.getSchemas();
    }

    @Override
    public String getTable() {
        return config.getTable();
    }

    @Override
    public @Nullable String getOldTable() {
        return config.getOldTable();
    }

    @Override
    public String getTablespace() {
        return config.getTablespace();
    }

    @Override
    public LogSystem getLogger() {
        return config.getLogger();
    }

    @Override
    public TargetVersion getTarget() {
        return config.getTarget();
    }

    @Override
    public boolean getFailOnMissingTarget() {
        return config.getFailOnMissingTarget();
    }

    @Override
    public MigrationPattern[] getCherryPick() {
        return config.getCherryPick();
    }

    @Override
    public boolean isPlaceholderReplacement() {
        return config.isPlaceholderReplacement();
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return config.getPlaceholders();
    }

    @Override
    public String getPlaceholderPrefix() {
        return config.getPlaceholderPrefix();
    }

    @Override
    public String getPlaceholderSuffix() {
        return config.getPlaceholderSuffix();
    }

    @Override
    public String getScriptPlaceholderPrefix() {
        return config.getScriptPlaceholderPrefix();
    }

    @Override
    public String getScriptPlaceholderSuffix() {
        return config.getScriptPlaceholderSuffix();
    }

    @Override
    public String getSqlMigrationPrefix() {
        return config.getSqlMigrationPrefix();
    }

    @Override
    public String getBaselineMigrationPrefix() {
        return config.getBaselineMigrationPrefix();
    }

    @Override
    public String getRepeatableSqlMigrationPrefix() {
        return config.getRepeatableSqlMigrationPrefix();
    }

    @Override
    public String getSqlMigrationSeparator() {
        return config.getSqlMigrationSeparator();
    }

    @Override
    public String[] getSqlMigrationSuffixes() {
        return config.getSqlMigrationSuffixes();
    }

    @Override
    public JavaMigration[] getJavaMigrations() {
        return config.getJavaMigrations();
    }

    @Override
    public boolean isIgnoreMissingMigrations() {
        return config.isIgnoreMissingMigrations();
    }

    @Override
    public boolean isIgnoreIgnoredMigrations() {
        return config.isIgnoreIgnoredMigrations();
    }

    @Override
    public boolean isIgnorePendingMigrations() {
        return config.isIgnorePendingMigrations();
    }

    @Override
    public boolean isIgnoreFutureMigrations() {
        return config.isIgnoreFutureMigrations();
    }

    @Override
    public ValidatePattern[] getIgnoreMigrationPatterns() {
        return config.getIgnoreMigrationPatterns();
    }

    @Override
    public boolean isValidateMigrationNaming() {
        return config.isValidateMigrationNaming();
    }

    @Override
    public boolean isValidateOnMigrate() {
        return config.isValidateOnMigrate();
    }

    @Override
    public boolean isCleanOnValidationError() {
        return config.isCleanOnValidationError();
    }

    @Override
    public boolean isCleanDisabled() {
        return config.isCleanDisabled();
    }

    @Override
    public Version getBaselineVersion() {
        return config.getBaselineVersion();
    }

    @Override
    public String getBaselineDescription() {
        return config.getBaselineDescription();
    }

    @Override
    public boolean isBaselineOnMigrate() {
        return config.isBaselineOnMigrate();
    }

    @Override
    public boolean isOutOfOrder() {
        return config.isOutOfOrder();
    }

    @Override
    public boolean isSkipExecutingMigrations() {
        return config.isSkipExecutingMigrations();
    }

    @Override
    public MigrationResolver[] getResolvers() {
        return config.getResolvers();
    }

    @Override
    public boolean isSkipDefaultResolvers() {
        return config.isSkipDefaultResolvers();
    }

    @Override
    public String getUrl() {
        return config.getUrl();
    }

    @Override
    public String getUser() {
        return config.getUser();
    }

    @Override
    public String getPassword() {
        return config.getPassword();
    }

    @Override
    public DataSource getDataSource() {
        return config.getDataSource();
    }

    @Override
    public int getConnectRetries() {
        return config.getConnectRetries();
    }

    @Override
    public int getConnectRetriesInterval() {
        return config.getConnectRetriesInterval();
    }

    @Override
    public String getInitSql() {
        return config.getInitSql();
    }

    @Override
    public ClassLoader getClassLoader() {
        return config.getClassLoader();
    }

    @Override
    public boolean isMixed() {
        return config.isMixed();
    }

    @Override
    public String getInstalledBy() {
        return config.getInstalledBy();
    }

    @Override
    public boolean isGroup() {
        return config.isGroup();
    }

    @Override
    public String[] getErrorOverrides() {
        return config.getErrorOverrides();
    }

    @Override
    public Supplier<OutputStream> getDryRunOutput() {
        return config.getDryRunOutput();
    }

    @Override
    public boolean isBatch() {
        return config.isBatch();
    }

    @Override
    public String getLicenseKey() {
        return config.getLicenseKey();
    }

    @Override
    public ResourceProvider getResourceProvider() {
        return config.getResourceProvider();
    }

    @Override
    public ClassProvider<JavaMigration> getJavaMigrationClassProvider() {
        return config.getJavaMigrationClassProvider();
    }

    @Override
    public boolean isOutputQueryResults() {
        return config.isOutputQueryResults();
    }

    @Override
    public boolean getCreateSchemas() {
        return config.getCreateSchemas();
    }

    @Override
    public int getLockRetryCount() {
        return config.getLockRetryCount();
    }

    @Override
    public Map<String, String> getJdbcProperties() {
        return config.getJdbcProperties();
    }

    @Override
    public boolean getFailOnMissingLocations() {
        return config.getFailOnMissingLocations();
    }

    @Override
    public DatabaseTypeRegister getDatabaseTypeRegister() {
        return config.getDatabaseTypeRegister();
    }

    @Override
    public Set<MigrateDbExtension> getLoadedExtensions() {
        return config.getLoadedExtensions();
    }

    @Override
    public Map<Class<? extends ExtensionConfig>, ? extends ExtensionConfig> getExtensionConfig() {
        return config.getExtensionConfig();
    }

    /**
     * Sets the stream where to output the SQL statements of a migration dry run. {@code null} to execute the SQL
     * statements directly against the database. The stream when be closing when MigrateDB finishes writing the output.
     *
     * @param dryRunOutput The output file or {@code null} to execute the SQL statements directly against the database.
     */
    public FluentConfiguration dryRunOutput(Supplier<OutputStream> dryRunOutput) {
        config.setDryRunOutput(dryRunOutput);
        return this;
    }

    /**
     * Sets the file where to output the SQL statements of a migration dry run. {@code null} to execute the SQL
     * statements directly against the database. If the file specified is in a non-existent directory, MigrateDB will
     * create all directories and parent directories as needed.
     *
     * @param dryRunOutputFileName The name of the output file or {@code null} to execute the SQL statements directly
     *                             against the database.
     */
    public FluentConfiguration dryRunOutput(String dryRunOutputFileName) {
        config.setDryRunOutputAsFileName(dryRunOutputFileName);
        return this;
    }

    /**
     * Rules for the built-in error handler that let you override specific SQL states and errors codes in order to force
     * specific errors or warnings to be treated as debug messages, info messages, warnings or errors.
     * <p>Each error override has the following format: {@code STATE:12345:W}.
     * It is a 5 character SQL state (or * to match all SQL states), a colon, the SQL error code (or * to match all SQL
     * error codes), a colon and finally the desired behavior that should override the initial one.</p>
     * <p>The following behaviors are accepted:</p>
     * <ul>
     * <li>{@code D} to force a debug message</li>
     * <li>{@code D-} to force a debug message, but do not show the original sql state and error code</li>
     * <li>{@code I} to force an info message</li>
     * <li>{@code I-} to force an info message, but do not show the original sql state and error code</li>
     * <li>{@code W} to force a warning</li>
     * <li>{@code W-} to force a warning, but do not show the original sql state and error code</li>
     * <li>{@code E} to force an error</li>
     * <li>{@code E-} to force an error, but do not show the original sql state and error code</li>
     * </ul>
     * <p>Example 1: to force Oracle stored procedure compilation issues to produce
     * errors instead of warnings, the following errorOverride can be used: {@code 99999:17110:E}</p>
     * <p>Example 2: to force SQL Server PRINT messages to be displayed as info messages (without SQL state and error
     * code details) instead of warnings, the following errorOverride can be used: {@code S0001:0:I-}</p>
     * <p>Example 3: to force all errors with SQL error code 123 to be treated as warnings instead,
     * the following errorOverride can be used: {@code *:123:W}</p>
     *
     * @param errorOverrides The ErrorOverrides or an empty array if none are defined. (default: none)
     */
    public FluentConfiguration errorOverrides(String... errorOverrides) {
        config.setErrorOverrides(errorOverrides);
        return this;
    }

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for
     * databases with support for DDL transactions).
     *
     * @param group {@code true} if migrations should be grouped. {@code false} if they should be applied individually
     *              instead. (default: {@code false})
     */
    public FluentConfiguration group(boolean group) {
        config.setGroup(group);
        return this;
    }

    /**
     * The username that will be recorded in the schema history table as having applied the migration.
     *
     * @param installedBy The username or {@code null} for the current database user of the connection. (default:
     *                    {@code null}).
     */
    public FluentConfiguration installedBy(String installedBy) {
        config.setInstalledBy(installedBy);
        return this;
    }

    /**
     * The logger MigrateDB should use.
     */
    public FluentConfiguration logger(LogSystem logger) {
        config.setLogger(logger);
        return this;
    }

    /**
     * The logger(s) MigrateDB should use.
     */
    public FluentConfiguration logger(String... logger) {
        return logger(LogSystems.fromStrings(Set.of(logger), getClassLoader(), null));
    }

    /**
     * Whether to allow mixing transactional and non-transactional statements within the same migration. Enabling this
     * automatically causes the entire affected migration to be run without a transaction.
     * <p>
     * Note that this is only applicable for PostgreSQL, Aurora PostgreSQL, SQL Server and SQLite which all have
     * statements that do not run at all within a transaction. This is not to be confused with implicit transaction, as
     * they occur in MySQL or Oracle, where even though a DDL statement was run within a transaction, the database will
     * issue an implicit commit before and after its execution.
     *
     * @param mixed {@code true} if mixed migrations should be allowed. {@code false} if an error should be thrown
     *              instead. (default: {@code false})
     */
    public FluentConfiguration mixed(boolean mixed) {
        config.setMixed(mixed);
        return this;
    }

    /**
     * Ignore missing migrations when reading the schema history table. These are migrations that were performed by an
     * older deployment of the application that are no longer available in this version. For example: we have migrations
     * available on the classpath with versions 1.0 and 3.0. The schema history table indicates that a migration with
     * version 2.0 (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDB continues normally. This is useful for situations where one must be able to
     * deploy a newer version of the application even though it doesn't contain migrations included with an older one
     * anymore. Note that if the most recently applied migration is removed, MigrateDb has no way to know it is missing
     * and will mark it as future instead.
     *
     * @param ignoreMissingMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast
     *                                with an exception. (default: {@code false})
     */
    public FluentConfiguration ignoreMissingMigrations(boolean ignoreMissingMigrations) {
        config.setIgnoreMissingMigrations(ignoreMissingMigrations);
        return this;
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
     *
     * @param ignoreIgnoredMigrations {@code true} to continue normally, {@code false} to fail fast with an exception.
     *                                (default: {@code false})
     */
    public FluentConfiguration ignoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
        config.setIgnoreIgnoredMigrations(ignoreIgnoredMigrations);
        return this;
    }

    /**
     * Ignore pending migrations when reading the schema history table. These are migrations that are available but have
     * not yet been applied. This can be useful for verifying that in-development migration changes don't contain any
     * validation-breaking changes of migrations that have already been applied to a production environment, e.g. as
     * part of a CI/CD process, without failing because of the existence of new migration versions.
     *
     * @param ignorePendingMigrations {@code true} to continue normally, {@code false} to fail fast with an exception.
     *                                (default: {@code false})
     */
    public FluentConfiguration ignorePendingMigrations(boolean ignorePendingMigrations) {
        config.setIgnorePendingMigrations(ignorePendingMigrations);
        return this;
    }

    /**
     * Whether to ignore future migrations when reading the schema history table. These are migrations that were
     * performed by a newer deployment of the application that are not yet available in this version. For example: we
     * have migrations available on the classpath up to version 3.0. The schema history table indicates that a migration
     * to version 4.0 (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDB continues normally. This is useful for situations where one must be able to
     * redeploy an older version of the application after the database has been migrated by a newer one.
     *
     * @param ignoreFutureMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast
     *                               with an exception. (default: {@code true})
     */
    public FluentConfiguration ignoreFutureMigrations(boolean ignoreFutureMigrations) {
        config.setIgnoreFutureMigrations(ignoreFutureMigrations);
        return this;
    }

    /**
     * Ignore migrations that match this comma-separated list of patterns when validating migrations. Each pattern is of
     * the form {@code <migration_type>:<migration_state>}. See <a
     * href="https://daniel-huss.github.io/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns">the
     * website</a> for full details.
     *
     * <p>Example: repeatable:missing,versioned:pending,*:failed
     */
    public FluentConfiguration ignoreMigrationPatterns(String... ignoreMigrationPatterns) {
        config.setIgnoreMigrationPatterns(ignoreMigrationPatterns);
        return this;
    }

    /**
     * Ignore migrations that match this array of ValidatePatterns when validating migrations.
     */
    public FluentConfiguration ignoreMigrationPatterns(ValidatePattern... ignoreMigrationPatterns) {
        config.setIgnoreMigrationPatterns(ignoreMigrationPatterns);
        return this;
    }

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure
     * can be useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     *
     * @param validateMigrationNaming {@code false} to continue normally, {@code true} to fail fast with an exception.
     *                                (default: {@code false})
     */
    public FluentConfiguration validateMigrationNaming(boolean validateMigrationNaming) {
        config.setValidateMigrationNaming(validateMigrationNaming);
        return this;
    }

    /**
     * Whether to automatically call validate or not when running migrate.
     *
     * @param validateOnMigrate {@code true} if validate should be called. {@code false} if not. (default:
     *                          {@code true})
     */
    public FluentConfiguration validateOnMigrate(boolean validateOnMigrate) {
        config.setValidateOnMigrate(validateOnMigrate);
        return this;
    }

    /**
     * Whether to automatically call clean or not when a validation error occurs. This is exclusively intended as a
     * convenience for development. even though we strongly recommend not to change migration scripts once they have
     * been checked into SCM and run, this provides a way of dealing with this case in a smooth manner. The database
     * will be wiped clean automatically, ensuring that the next migration will bring you back to the state checked into
     * SCM.
     * <b>Warning! Do not enable in production!</b>
     *
     * @param cleanOnValidationError {@code true} if clean should be called. {@code false} if not. (default:
     *                               {@code false})
     */
    public FluentConfiguration cleanOnValidationError(boolean cleanOnValidationError) {
        config.setCleanOnValidationError(cleanOnValidationError);
        return this;
    }

    /**
     * Whether to disable clean. This is especially useful for production environments where running clean can be quite
     * a career limiting move.
     *
     * @param cleanDisabled {@code true} to disable clean. {@code false} to leave it enabled.  (default: {@code false})
     */
    public FluentConfiguration cleanDisabled(boolean cleanDisabled) {
        config.setCleanDisabled(cleanDisabled);
        return this;
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     *
     * @param locations Locations to scan recursively for migrations. (default: db/migration)
     */
    public FluentConfiguration locations(String... locations) {
        config.setLocationsAsStrings(locations);
        return this;
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     *
     * @param locations Locations to scan recursively for migrations. (default: db/migration)
     */
    public FluentConfiguration locations(Location... locations) {
        config.setLocations(locations);
        return this;
    }

    /**
     * Sets the encoding of SQL migrations.
     *
     * @param encoding The encoding of SQL migrations. (default: UTF-8)
     */
    public FluentConfiguration encoding(String encoding) {
        config.setEncodingAsString(encoding);
        return this;
    }

    /**
     * Sets the encoding of SQL migrations.
     *
     * @param encoding The encoding of SQL migrations. (default: UTF-8)
     */
    public FluentConfiguration encoding(Charset encoding) {
        config.setEncoding(encoding);
        return this;
    }

    /**
     * Sets the default schema managed by MigrateDB. This schema name is case-sensitive. If not specified, but
     * <i>schemas</i> is, MigrateDB uses the first schema in that list. If that is also not specified, MigrateDB uses
     * the default schema for the database connection.
     * <p>Consequences:</p>
     * <ul>
     * <li>This schema will be the one containing the schema history table.</li>
     * <li>This schema will be the default for the database connection (provided the database supports this concept)
     * .</li>
     * </ul>
     *
     * @param schema The default schema managed by MigrateDb.
     */
    public FluentConfiguration defaultSchema(String schema) {
        config.setDefaultSchema(schema);
        return this;
    }

    /**
     * Sets the schemas managed by MigrateDB. These schema names are case-sensitive. If not specified, MigrateDB uses
     * the default schema for the database connection. If <i>defaultSchemaName</i> is not specified, then the first of
     * this list also acts as default schema.
     * <p>Consequences:</p>
     * <ul>
     * <li>MigrateDB will automatically attempt to create all these schemas, unless they already exist.</li>
     * <li>The schemas will be cleaned in the order of this list.</li>
     * <li>If MigrateDB created them, the schemas themselves will be dropped when cleaning.</li>
     * </ul>
     *
     * @param schemas The schemas managed by MigrateDb. May not be {@code null}. Must contain at least one element.
     */
    public FluentConfiguration schemas(String... schemas) {
        config.setSchemas(schemas);
        return this;
    }

    /**
     * Sets the name of the schema history table that will be used by MigrateDB. By default (single-schema mode) the
     * schema history table is placed in the default schema for the connection provided by the datasource. When the
     * <i>migratedb.schemas</i> property is set (multi-schema mode), the schema history table is placed in the first
     * schema of the list.
     *
     * @param table The name of the schema history table that will be used by MigrateDB. (default:
     *              migratedb_state)
     */
    public FluentConfiguration table(String table) {
        config.setTable(table);
        return this;
    }

    /**
     * Sets the name of the old table to convert into the format used by MigrateDB. Only used for the "liberate" command.
     *
     * @param oldTable Name of old table to convert.
     */
    public FluentConfiguration oldTable(@Nullable String oldTable) {
        config.setOldTable(oldTable);
        return this;
    }

    /**
     * Sets the tablespace where to create the schema history table that will be used by MigrateDB. If not specified,
     * MigrateDB uses the default tablespace for the database connection. This setting is only relevant for databases
     * that do support the notion of tablespaces. Its value is simply ignored for all others.
     *
     * @param tablespace The tablespace where to create the schema history table that will be used by MigrateDb.
     */
    public FluentConfiguration tablespace(String tablespace) {
        config.setTablespace(tablespace);
        return this;
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
    public FluentConfiguration target(TargetVersion target) {
        config.setTarget(target);
        return this;
    }

    /**
     * Sets the target version up to which MigrateDB should consider migrations.
     */
    public FluentConfiguration target(Version target) {
        config.setTarget(TargetVersion.of(target));
        return this;
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
    public FluentConfiguration target(String target) {
        config.setTargetAsString(target);
        return this;
    }

    /**
     * Gets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored.
     */
    public FluentConfiguration cherryPick(MigrationPattern... cherryPick) {
        config.setCherryPick(cherryPick);
        return this;
    }

    /**
     * Gets the migrations that MigrateDB should consider when migrating. Leave empty to consider all available
     * migrations. Migrations not in this list will be ignored. Values should be the version for versioned migrations
     * (e.g. 1, 2.4, 6.5.3) or the description for repeatable migrations (e.g. Insert_Data, Create_Table)
     */
    public FluentConfiguration cherryPick(String... cherryPickAsString) {
        config.setCherryPick(cherryPickAsString);
        return this;
    }

    /**
     * Sets whether placeholders should be replaced.
     *
     * @param placeholderReplacement Whether placeholders should be replaced. (default: true)
     */
    public FluentConfiguration placeholderReplacement(boolean placeholderReplacement) {
        config.setPlaceholderReplacement(placeholderReplacement);
        return this;
    }

    /**
     * Sets the placeholders to replace in sql migration scripts.
     *
     * @param placeholders The map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    public FluentConfiguration placeholders(Map<String, String> placeholders) {
        config.setPlaceholders(placeholders);
        return this;
    }

    /**
     * Sets the prefix of every placeholder.
     *
     * @param placeholderPrefix The prefix of every placeholder. (default: ${ )
     */
    public FluentConfiguration placeholderPrefix(String placeholderPrefix) {
        config.setPlaceholderPrefix(placeholderPrefix);
        return this;
    }

    /**
     * Sets the suffix of every placeholder.
     *
     * @param placeholderSuffix The suffix of every placeholder. (default: } )
     */
    public FluentConfiguration placeholderSuffix(String placeholderSuffix) {
        config.setPlaceholderSuffix(placeholderSuffix);
        return this;
    }

    /**
     * Sets the prefix of every script placeholder.
     *
     * @param scriptPlaceholderPrefix The prefix of every placeholder. (default: FP__ )
     */
    public FluentConfiguration scriptPlaceholderPrefix(String scriptPlaceholderPrefix) {
        config.setScriptPlaceholderPrefix(scriptPlaceholderPrefix);
        return this;
    }

    /**
     * Sets the suffix of every script placeholder.
     *
     * @param scriptPlaceholderSuffix The suffix of every script placeholder. (default: __ )
     */
    public FluentConfiguration scriptPlaceholderSuffix(String scriptPlaceholderSuffix) {
        config.setScriptPlaceholderSuffix(scriptPlaceholderSuffix);
        return this;
    }

    /**
     * Sets the file name prefix for sql migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     *
     * @param sqlMigrationPrefix The file name prefix for sql migrations (default: V)
     */
    public FluentConfiguration sqlMigrationPrefix(String sqlMigrationPrefix) {
        config.setSqlMigrationPrefix(sqlMigrationPrefix);
        return this;
    }

    /**
     * Sets the file name prefix for baseline migrations. They have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to B1.1__My_description.sql
     *
     * @param baselineMigrationPrefix The file name prefix for baseline migrations (default: B)
     */
    public FluentConfiguration baselineMigrationPrefix(String baselineMigrationPrefix) {
        config.setBaselineMigrationPrefix(baselineMigrationPrefix);
        return this;
    }

    /**
     * Sets the file name prefix for repeatable sql migrations. Repeatable SQL migrations have the following file name
     * structure: prefixSeparatorDESCRIPTIONsuffix, which using the defaults translates to R__My_description.sql
     *
     * @param repeatableSqlMigrationPrefix The file name prefix for repeatable sql migrations (default: R)
     */
    public FluentConfiguration repeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
        config.setRepeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix);
        return this;
    }

    /**
     * Sets the file name separator for sql migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     *
     * @param sqlMigrationSeparator The file name separator for sql migrations (default: __)
     */
    public FluentConfiguration sqlMigrationSeparator(String sqlMigrationSeparator) {
        config.setSqlMigrationSeparator(sqlMigrationSeparator);
        return this;
    }

    /**
     * The file name suffixes for SQL migrations. (default: .sql) SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql Multiple
     * suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
     * specific file associations.
     *
     * @param sqlMigrationSuffixes The file name suffixes for SQL migrations.
     */
    public FluentConfiguration sqlMigrationSuffixes(String... sqlMigrationSuffixes) {
        config.setSqlMigrationSuffixes(sqlMigrationSuffixes);
        return this;
    }

    /**
     * The additional Java-based migrations. These are not Java-based migrations discovered through classpath
     * scanning and instantiated by MigrateDB. Instead these are application-controlled instances of JavaMigration. This
     * is particularly useful when working with a dependency injection container, where you may want the DI container to
     * instantiate the class and wire up its dependencies for you.
     *
     * @param javaMigrations The additional Java-based migrations. An empty array if none. (default: none)
     */
    public FluentConfiguration javaMigrations(JavaMigration... javaMigrations) {
        config.setJavaMigrations(Arrays.asList(javaMigrations));
        return this;
    }

    /**
     * The additional Java-based migrations. These are not Java-based migrations discovered through classpath
     * scanning and instantiated by MigrateDB. Instead these are application-controlled instances of JavaMigration. This
     * is particularly useful when working with a dependency injection container, where you may want the DI container to
     * instantiate the class and wire up its dependencies for you.
     *
     * @param javaMigrations The additional Java-based migrations. An empty array if none. (default: none)
     */
    public FluentConfiguration javaMigrations(Collection<JavaMigration> javaMigrations) {
        config.setJavaMigrations(javaMigrations);
        return this;
    }

    /**
     * Sets the datasource to use. Must have the necessary privileges to execute DDL.
     *
     * @param dataSource The datasource to use. Must have the necessary privileges to execute DDL.
     */
    public FluentConfiguration dataSource(DataSource dataSource) {
        config.setDataSource(dataSource);
        return this;
    }

    /**
     * Sets the datasource to use. Must have the necessary privileges to execute DDL.
     *
     * @param url      The JDBC URL of the database.
     * @param user     The user of the database.
     * @param password The password of the database.
     */
    public FluentConfiguration dataSource(String url, String user, String password) {
        config.setDataSource(url, user, password);
        return this;
    }

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, MigrateDB
     * will wait 1 second before attempting to connect again, up to the maximum number of times specified by
     * connectRetries. The interval between retries doubles with each subsequent attempt.
     *
     * @param connectRetries The maximum number of retries (default: 0).
     */
    public FluentConfiguration connectRetries(int connectRetries) {
        config.setConnectRetries(connectRetries);
        return this;
    }

    /**
     * The maximum time between retries when attempting to connect to the database in seconds. This will cap the
     * interval between connect retry to the value provided.
     *
     * @param connectRetriesInterval The maximum time between retries in seconds (default: 120).
     */
    public FluentConfiguration connectRetriesInterval(int connectRetriesInterval) {
        config.setConnectRetriesInterval(connectRetriesInterval);
        return this;
    }

    /**
     * The SQL statements to run to initialize a new database connection immediately after opening it.
     *
     * @param initSql The SQL statements. (default: {@code null})
     */
    public FluentConfiguration initSql(String initSql) {
        config.setInitSql(initSql);
        return this;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     *
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public FluentConfiguration baselineVersion(Version baselineVersion) {
        config.setBaselineVersion(baselineVersion);
        return this;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     *
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public FluentConfiguration baselineVersion(String baselineVersion) {
        config.setBaselineVersion(Version.parse(baselineVersion));
        return this;
    }

    /**
     * Sets the description to tag an existing schema with when executing baseline.
     *
     * @param baselineDescription The description to tag an existing schema with when executing baseline. (default:
     *                            &lt;&lt; MigrateDb Baseline &gt;&gt;)
     */
    public FluentConfiguration baselineDescription(String baselineDescription) {
        config.setBaselineDescription(baselineDescription);
        return this;
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
     *
     * @param baselineOnMigrate {@code true} if baseline should be called on migrate for non-empty schemas,
     *                          {@code false} if not. (default: {@code false})
     */
    public FluentConfiguration baselineOnMigrate(boolean baselineOnMigrate) {
        config.setBaselineOnMigrate(baselineOnMigrate);
        return this;
    }

    /**
     * Allows migrations to be run "out of order". If you already have versions 1 and 3 applied, and now a version 2 is
     * found, it will be applied too instead of being ignored.
     *
     * @param outOfOrder {@code true} if outOfOrder migrations should be applied, {@code false} if not. (default:
     *                   {@code false})
     */
    public FluentConfiguration outOfOrder(boolean outOfOrder) {
        config.setOutOfOrder(outOfOrder);
        return this;
    }

    /**
     * Whether MigrateDB should skip actually executing the contents of the migrations and only update the schema
     * history table. This should be used when you have applied a migration manually (via executing the sql yourself, or
     * via an ide), and just want the schema history table to reflect this.
     * <p>
     * Use in conjunction with {@code cherryPick} to skip specific migrations instead of all pending ones.
     */
    public FluentConfiguration skipExecutingMigrations(boolean skipExecutingMigrations) {
        config.setSkipExecutingMigrations(skipExecutingMigrations);
        return this;
    }

    /**
     * Gets the callbacks for lifecycle notifications.
     *
     * @return The callbacks for lifecycle notifications. An empty array if none. (default: none)
     */
    @Override
    public Callback[] getCallbacks() {
        return config.getCallbacks();
    }

    @Override
    public boolean isSkipDefaultCallbacks() {
        return config.isSkipDefaultCallbacks();
    }

    /**
     * Set the callbacks for lifecycle notifications.
     *
     * @param callbacks The callbacks for lifecycle notifications. (default: none)
     */
    public FluentConfiguration callbacks(Callback... callbacks) {
        config.setCallbacks(callbacks);
        return this;
    }

    /**
     * Set the callbacks for lifecycle notifications.
     *
     * @param callbacks The fully qualified class names, or full qualified package to scan, of the callbacks for
     *                  lifecycle notifications. (default: none)
     */
    public FluentConfiguration callbacks(String... callbacks) {
        config.setCallbacksAsClassNames(callbacks);
        return this;
    }

    /**
     * Whether MigrateDB should skip the default callbacks. If true, only custom callbacks are used.
     *
     * @param skipDefaultCallbacks Whether default built-in callbacks should be skipped. (default: false)
     */
    public FluentConfiguration skipDefaultCallbacks(boolean skipDefaultCallbacks) {
        config.setSkipDefaultCallbacks(skipDefaultCallbacks);
        return this;
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     *
     * @param resolvers The custom MigrationResolvers to be used in addition to the built-in ones for resolving
     *                  Migrations to apply. (default: empty list)
     */
    public FluentConfiguration resolvers(MigrationResolver... resolvers) {
        config.setResolvers(resolvers);
        return this;
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     *
     * @param resolvers The fully qualified class names of the custom MigrationResolvers to be used in addition to the
     *                  built-in ones for resolving Migrations to apply. (default: empty list)
     */
    public FluentConfiguration resolvers(String... resolvers) {
        config.setResolversAsClassNames(resolvers);
        return this;
    }

    /**
     * Whether MigrateDB should skip the default resolvers. If true, only custom resolvers are used.
     *
     * @param skipDefaultResolvers Whether default built-in resolvers should be skipped. (default: false)
     */
    public FluentConfiguration skipDefaultResolvers(boolean skipDefaultResolvers) {
        config.setSkipDefaultResolvers(skipDefaultResolvers);
        return this;
    }

    /**
     * <b>Note: Currently not implemented.</b>
     * Whether to batch SQL statements when executing them. Batching can save up to 99 percent of network roundtrips by
     * sending up to 100 statements at once over the network to the database, instead of sending each statement
     * individually. This is particularly useful for very large SQL migrations composed of multiple MB or even GB of
     * reference data, as this can dramatically reduce the network overhead. This is supported for INSERT, UPDATE,
     * DELETE, MERGE and UPSERT statements. All other statements are automatically executed without batching.
     *
     * @param batch {@code true} to batch SQL statements. {@code false} to execute them individually instead. (default:
     *              {@code false})
     */
    public FluentConfiguration batch(boolean batch) {
        config.setBatch(batch);
        return this;
    }

    public FluentConfiguration lockRetryCount(int lockRetryCount) {
        config.setLockRetryCount(lockRetryCount);
        return this;
    }

    /**
     * Properties to pass to the JDBC driver object
     *
     * @param jdbcProperties The properties to pass to the JDBC driver object
     */
    public FluentConfiguration jdbcProperties(Map<String, String> jdbcProperties) {
        config.setJdbcProperties(jdbcProperties);
        return this;
    }

    /**
     * Sets the extension config of type {@code T}.
     */
    public <T extends ExtensionConfig> FluentConfiguration extensionConfig(Class<T> extensionConfigType, T value) {
        config.setExtensionConfig(extensionConfigType, value);
        return this;
    }

    /**
     * Custom ResourceProvider to be used to look up resources. If not set, the default strategy will be used.
     *
     * @param resourceProvider Custom ResourceProvider to be used to look up resources
     */
    public FluentConfiguration resourceProvider(ResourceProvider resourceProvider) {
        config.setResourceProvider(resourceProvider);
        return this;
    }

    /**
     * Custom ClassProvider to be used to look up {@link JavaMigration} classes. If not set, the default strategy will
     * be used.
     *
     * @param javaMigrationClassProvider Custom ClassProvider to be used to look up {@link JavaMigration} classes.
     */
    public FluentConfiguration javaMigrationClassProvider(ClassProvider<JavaMigration> javaMigrationClassProvider) {
        config.setJavaMigrationClassProvider(javaMigrationClassProvider);
        return this;
    }

    /**
     * Whether MigrateDB should output a table with the results of queries when executing migrations.
     *
     * @param outputQueryResults {@code true} to output a table with the results of queries when executing migrations.
     *                           (default: {@code true})
     */
    public FluentConfiguration outputQueryResults(boolean outputQueryResults) {
        config.setOutputQueryResults(outputQueryResults);
        return this;
    }

    /**
     * Configures MigrateDB with these properties. This overwrites any existing configuration. Property names are
     * documented in {@link PropertyNames}. To use a custom ClassLoader, setClassLoader() must be called prior to
     * calling this method. To support the configuration of extensions, those extensions must be activated via
     * {@code useExtension} prior to calling this method.
     *
     * @param properties Properties used for configuration.
     * @throws MigrateDbException when the configuration failed.
     */
    public FluentConfiguration configuration(Properties properties) {
        config.configure(properties);
        return this;
    }

    /**
     * Configures MigrateDB with these properties. This overwrites any existing configuration. Property names are
     * documented in {@link PropertyNames}. To use a custom ClassLoader, it must be passed to the MigrateDb constructor
     * prior to calling this method. To support the configuration of extensions, those extensions must be activated via
     * {@code useExtension} prior to calling this method.
     *
     * @param props Properties used for configuration.
     * @throws MigrateDbException when the configuration failed.
     */
    public FluentConfiguration configuration(Map<String, String> props) {
        config.configure(props);
        return this;
    }

    /**
     * Whether MigrateDB should attempt to create the schemas specified in the schemas property
     *
     * @param createSchemas {@code true} to attempt to create the schemas (default: {@code true})
     */
    public FluentConfiguration createSchemas(boolean createSchemas) {
        config.setShouldCreateSchemas(createSchemas);
        return this;
    }

    /**
     * Whether to fail if a location specified in the migratedb.locations option doesn't exist
     *
     * @return {@code true} to fail (default: {@code false})
     */
    public FluentConfiguration failOnMissingLocations(boolean failOnMissingLocations) {
        config.setFailOnMissingLocations(failOnMissingLocations);
        return this;
    }

    /**
     * Enables a single MigrateDB extension.
     */
    public FluentConfiguration useExtension(MigrateDbExtension extension) {
        config.useExtension(extension);
        return this;
    }

    /**
     * Enables multiple MigrateDB extensions. This is mainly used to load extensions from the {@code ServiceLoader}
     * facility:
     * <pre>{@code
     *      config.useExtensions(ServiceLoader.load(MigrateDbExtension.class, someClassLoader))
     * }</pre>
     */
    public FluentConfiguration useExtensions(Iterable<MigrateDbExtension> extensions) {
        config.useExtensions(extensions);
        return this;
    }
}
