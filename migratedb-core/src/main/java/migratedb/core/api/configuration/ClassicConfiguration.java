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

import static migratedb.core.internal.configuration.ConfigUtils.removeBoolean;
import static migratedb.core.internal.configuration.ConfigUtils.removeInteger;
import static migratedb.core.internal.util.Development.TODO;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import migratedb.core.api.ClassProvider;
import migratedb.core.api.DatabaseTypeRegister;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.Location;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrateDbExtension;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.MigrationVersion;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.callback.Callback;
import migratedb.core.api.logging.LogSystem;
import migratedb.core.api.logging.LogSystems;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.api.pattern.ValidatePattern;
import migratedb.core.api.resolver.MigrationResolver;
import migratedb.core.internal.configuration.ConfigUtils;
import migratedb.core.internal.database.DatabaseTypeRegisterImpl;
import migratedb.core.internal.extension.BuiltinFeatures;
import migratedb.core.internal.jdbc.DriverDataSource;
import migratedb.core.internal.util.ClassUtils;
import migratedb.core.internal.util.Locations;
import migratedb.core.internal.util.StringUtils;

/**
 * JavaBean-style configuration for MigrateDB. This is primarily meant for compatibility with scenarios where the new
 * FluentConfiguration isn't an easy fit, such as Spring XML bean configuration.
 * <p>This configuration can then be passed to MigrateDB using the {@code new MigrateDb(Configuration)}
 * constructor.</p>
 */
public class ClassicConfiguration implements Configuration {
    private String driver;
    private String url;
    private String user;
    private String password;
    private DataSource dataSource;
    private int connectRetries;
    private int connectRetriesInterval = 120;
    private String initSql;
    private final ClassLoader classLoader;
    private Locations locations;
    private Charset encoding = StandardCharsets.UTF_8;
    private String defaultSchemaName = null;
    private String[] schemaNames = {};
    private String table = "migratedb_schema_history";
    private String tablespace;
    private MigrationVersion target;
    private boolean failOnMissingTarget = true;
    private MigrationPattern[] cherryPick;
    private boolean placeholderReplacement = true;
    private Map<String, String> placeholders = new HashMap<>();
    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private String scriptPlaceholderPrefix = "FP__";
    private String scriptPlaceholderSuffix = "__";
    private String sqlMigrationPrefix = "V";
    private String baselineMigrationPrefix = "B";
    private String undoSqlMigrationPrefix = "U";
    private String repeatableSqlMigrationPrefix = "R";
    private ResourceProvider resourceProvider = null;
    private ClassProvider<JavaMigration> javaMigrationClassProvider = null;
    private String sqlMigrationSeparator = "__";
    private String[] sqlMigrationSuffixes = { ".sql" };
    private JavaMigration[] javaMigrations = {};
    private boolean ignoreMissingMigrations;
    private boolean ignoreIgnoredMigrations;
    private boolean ignorePendingMigrations;
    private boolean ignoreFutureMigrations = true;
    private ValidatePattern[] ignoreMigrationPatterns = new ValidatePattern[0];
    private boolean validateMigrationNaming = false;
    private boolean validateOnMigrate = true;
    private boolean cleanOnValidationError;
    private boolean cleanDisabled = true;
    private MigrationVersion baselineVersion = MigrationVersion.fromVersion("1");
    private String baselineDescription = "<< MigrateDB Baseline >>";
    private boolean baselineOnMigrate;
    private boolean outOfOrder;
    private boolean skipExecutingMigrations;
    private final List<Callback> callbacks = new ArrayList<>();
    private boolean skipDefaultCallbacks;
    private MigrationResolver[] resolvers = new MigrationResolver[0];
    private boolean skipDefaultResolvers;
    private boolean mixed;
    private boolean group;
    private String installedBy;
    private boolean createSchemas = true;
    private String[] errorOverrides = new String[0];
    private OutputStream dryRunOutput;
    private boolean batch;
    private boolean outputQueryResults = true;
    private int lockRetryCount = 50;
    private Map<String, String> jdbcProperties;
    private boolean oracleSqlplus;
    private boolean oracleSqlplusWarn;
    private String oracleKerberosConfigFile = "";
    private String oracleKerberosCacheFile = "";
    private String oracleWalletLocation;
    private boolean failOnMissingLocations = false;
    private LogSystem logger;
    private final DatabaseTypeRegisterImpl databaseTypeRegister = new DatabaseTypeRegisterImpl();
    private final Set<MigrateDbExtension> loadedExtensions = new HashSet<>();

    public ClassicConfiguration() {
        this(ClassUtils.defaultClassLoader());
    }

    /**
     * @param classLoader The ClassLoader to use for loading migrations, resolvers, etc from the classpath. (default:
     *                    Thread.currentThread().getContextClassLoader()). Nullable for compatibility.
     */
    public ClassicConfiguration(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader == null ? ClassUtils.defaultClassLoader() : classLoader;
        this.locations = new Locations(List.of("db/migration"), classLoader);
        useExtension(BuiltinFeatures.instance());
    }

    /**
     * Creates a new configuration with the same values as this existing one.
     */
    public ClassicConfiguration(Configuration configuration) {
        this(configuration.getClassLoader());
        configure(configuration);
    }

    @Override
    public Location[] getLocations() {
        return locations.getLocations().toArray(new Location[0]);
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
    public String[] getSchemas() {
        return schemaNames;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public String getTablespace() {
        return tablespace;
    }

    @Override
    public MigrationVersion getTarget() {
        return target;
    }

    @Override
    public boolean getFailOnMissingTarget() {
        return failOnMissingTarget;
    }

    @Override
    public MigrationPattern[] getCherryPick() {
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
    public String[] getSqlMigrationSuffixes() {
        return sqlMigrationSuffixes;
    }

    @Override
    public JavaMigration[] getJavaMigrations() {
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
    public ValidatePattern[] getIgnoreMigrationPatterns() {
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
    public boolean isCleanOnValidationError() {
        return cleanOnValidationError;
    }

    @Override
    public boolean isCleanDisabled() {
        return cleanDisabled;
    }

    @Override
    public MigrationVersion getBaselineVersion() {
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
    public MigrationResolver[] getResolvers() {
        return resolvers;
    }

    @Override
    public boolean isSkipDefaultResolvers() {
        return skipDefaultResolvers;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public DataSource getDataSource() {
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
    public String[] getErrorOverrides() {
        return errorOverrides;
    }

    @Override
    public OutputStream getDryRunOutput() {
        return dryRunOutput;
    }

    @Override
    public String getLicenseKey() {
        return null;
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
    public Map<String, String> getJdbcProperties() {
        return jdbcProperties;
    }

    @Override
    public boolean getFailOnMissingLocations() {
        return failOnMissingLocations;
    }

    @Override
    public boolean outputQueryResults() {
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
    public boolean getCreateSchemas() {
        return createSchemas;
    }

    @Override
    public boolean isBatch() {
        return batch;
    }

    @Override
    public String getUndoSqlMigrationPrefix() {
        return undoSqlMigrationPrefix;
    }

    @Override
    public Callback[] getCallbacks() {
        return callbacks.toArray(new Callback[0]);
    }

    @Override
    public boolean isSkipDefaultCallbacks() {
        return skipDefaultCallbacks;
    }

    @Override
    public boolean isOracleSqlplus() {
        return oracleSqlplus;
    }

    @Override
    public boolean isOracleSqlplusWarn() {
        return oracleSqlplusWarn;
    }

    @Override
    public String getOracleKerberosConfigFile() {
        return oracleKerberosConfigFile;
    }

    @Override
    public String getOracleKerberosCacheFile() {
        return oracleKerberosCacheFile;
    }

    @Override
    public DatabaseTypeRegister getDatabaseTypeRegister() {
        return databaseTypeRegister;
    }

    /**
     * Sets the stream where to output the SQL statements of a migration dry run. {@code null} to execute the SQL
     * statements directly against the database. The stream will be closed when MigrateDb finishes writing the output.
     *
     * @param dryRunOutput The output file or {@code null} to execute the SQL statements directly against the database.
     */
    public void setDryRunOutput(OutputStream dryRunOutput) {
        this.dryRunOutput = dryRunOutput;
    }

    /**
     * Sets the file where to output the SQL statements of a migration dry run. {@code null} to execute the SQL
     * statements directly against the database. If the file specified is in a non-existent directory, MigrateDb will
     * create all directories and parent directories as needed.
     * <i>MigrateDb Teams only</i>
     *
     * @param dryRunOutput The output file or {@code null} to execute the SQL statements directly against the database.
     */
    public void setDryRunOutputAsFile(File dryRunOutput) {
        TODO("Implement seting dry run output as file");
    }

    /**
     * Sets the file where to output the SQL statements of a migration dry run. {@code null} to execute the SQL
     * statements directly against the database. If the file specified is in a non-existent directory, MigrateDb will
     * create all directories and parent directories as needed. Paths starting with s3: point to a bucket in AWS S3,
     * which must exist. They are in the format {@code s3:<bucket>(/optionalfolder/subfolder)/filename.sql}
     * <p>
     * Paths starting with gcs: point to a bucket in Google Cloud Storage, which must exist. They are in the format
     * {@code gcs:<bucket>(/optionalfolder/subfolder)/filename.sql}
     * <i>MigrateDb Teams only</i>
     *
     * @param dryRunOutputFileName The name of the output file or {@code null} to execute the SQL statements directly
     *                             against the database.
     */
    public void setDryRunOutputAsFileName(String dryRunOutputFileName) {
        TODO("Implement setting dry run output as file name");
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
    public void setErrorOverrides(String... errorOverrides) {
        this.errorOverrides = errorOverrides;
    }

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for
     * databases with support for DDL transactions).
     *
     * @param group {@code true} if migrations should be grouped. {@code false} if they should be applied individually
     *              instead. (default: {@code false})
     */
    public void setGroup(boolean group) {
        this.group = group;
    }

    /**
     * The username that will be recorded in the schema history table as having applied the migration.
     *
     * @param installedBy The username or {@code null} for the current database user of the connection. (default: {@code
     *                    null}).
     */
    public void setInstalledBy(String installedBy) {
        if ("".equals(installedBy)) {
            installedBy = null;
        }
        this.installedBy = installedBy;
    }

    /**
     * The log system MigrateDb should use.
     */
    public void setLogger(LogSystem logger) {
        this.logger = logger;
    }

    /**
     * The log system(s) MigrateDb should use.
     */
    public void setLogger(String... logger) {
        this.logger = LogSystems.parse(Set.of(logger), getClassLoader(), null);
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
    public void setMixed(boolean mixed) {
        this.mixed = mixed;
    }

    /**
     * Ignore missing migrations when reading the schema history table. These are migrations that were performed by an
     * older deployment of the application that are no longer available in this version. For example: we have migrations
     * available on the classpath with versions 1.0 and 3.0. The schema history table indicates that a migration with
     * version 2.0 (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDb continues normally. This is useful for situations where one must be able to
     * deploy a newer version of the application even though it doesn't contain migrations included with an older one
     * anymore. Note that if the most recently applied migration is removed, MigrateDb has no way to know it is missing
     * and will mark it as future instead.
     *
     * @param ignoreMissingMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast
     *                                with an exception. (default: {@code false})
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
     *
     * @param ignoreIgnoredMigrations {@code true} to continue normally, {@code false} to fail fast with an exception.
     *                                (default: {@code false})
     */
    public void setIgnoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
        this.ignoreIgnoredMigrations = ignoreIgnoredMigrations;
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
    public void setIgnorePendingMigrations(boolean ignorePendingMigrations) {
        this.ignorePendingMigrations = ignorePendingMigrations;
    }

    /**
     * Whether to ignore future migrations when reading the schema history table. These are migrations that were
     * performed by a newer deployment of the application that are not yet available in this version. For example: we
     * have migrations available on the classpath up to version 3.0. The schema history table indicates that a migration
     * to version 4.0 (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and MigrateDb continues normally. This is useful for situations where one must be able to
     * redeploy an older version of the application after the database has been migrated by a newer one.
     *
     * @param ignoreFutureMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast
     *                               with an exception. (default: {@code true})
     */
    public void setIgnoreFutureMigrations(boolean ignoreFutureMigrations) {
        this.ignoreFutureMigrations = ignoreFutureMigrations;
    }

    /**
     * Ignore migrations that match this comma-separated list of patterns when validating migrations. Each pattern is of
     * the form {@code <migration_type>:<migration_state>}. See https://no-website-yet.org/documentation/configuration
     * /parameters/ignoreMigrationPatterns for full details.
     * <p>Example: repeatable:missing,versioned:pending,*:failed
     */
    public void setIgnoreMigrationPatterns(String... ignoreMigrationPatterns) {
        this.ignoreMigrationPatterns = Arrays.stream(ignoreMigrationPatterns)
                                             .map(ValidatePattern::fromPattern)
                                             .toArray(ValidatePattern[]::new);
    }

    /**
     * Ignore migrations that match this array of ValidatePatterns when validating migrations.
     */
    public void setIgnoreMigrationPatterns(ValidatePattern... ignoreMigrationPatterns) {
        this.ignoreMigrationPatterns = ignoreMigrationPatterns;
    }

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure
     * can be useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     *
     * @param validateMigrationNaming {@code false} to continue normally, {@code true} to fail fast with an exception.
     *                                (default: {@code false})
     */
    public void setValidateMigrationNaming(boolean validateMigrationNaming) {
        this.validateMigrationNaming = validateMigrationNaming;
    }

    /**
     * Whether to automatically call validate or not when running migrate.
     *
     * @param validateOnMigrate {@code true} if validate should be called. {@code false} if not. (default: {@code
     *                          true})
     */
    public void setValidateOnMigrate(boolean validateOnMigrate) {
        this.validateOnMigrate = validateOnMigrate;
    }

    /**
     * Whether to automatically call clean or not when a validation error occurs. This is exclusively intended as a
     * convenience for development. even though we strongly recommend not to change migration scripts once they have
     * been checked into SCM and run, this provides a way of dealing with this case in a smooth manner. The database
     * will be wiped clean automatically, ensuring that the next migration will bring you back to the state checked into
     * SCM.
     * <b>Warning! Do not enable in production!</b>
     *
     * @param cleanOnValidationError {@code true} if clean should be called. {@code false} if not. (default: {@code
     *                               false})
     */
    public void setCleanOnValidationError(boolean cleanOnValidationError) {
        this.cleanOnValidationError = cleanOnValidationError;
    }

    /**
     * Whether to disable clean. This is especially useful for production environments where running clean can be quite
     * a career limiting move.
     *
     * @param cleanDisabled {@code true} to disable clean. {@code false} to leave it enabled.  (default: {@code false})
     */
    public void setCleanDisabled(boolean cleanDisabled) {
        this.cleanDisabled = cleanDisabled;
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     *
     * @param locations Locations to scan recursively for migrations. (default: db/migration)
     */
    public void setLocationsAsStrings(String... locations) {
        this.locations = new Locations(Arrays.asList(locations), classLoader);
    }

    /**
     * Sets the locations to scan recursively for migrations. The location type is determined by its prefix. Unprefixed
     * locations or locations starting with {@code classpath:} point to a package on the classpath and may contain both
     * SQL and Java-based migrations. Locations starting with {@code filesystem:} point to a directory on the
     * filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.
     *
     * @param locations Locations to scan recursively for migrations. (default: db/migration)
     */
    public void setLocations(Location... locations) {
        this.locations = new Locations(Arrays.asList(locations));
    }

    /**
     * Sets the encoding of SQL migrations.
     *
     * @param encoding The encoding of SQL migrations. (default: UTF-8)
     */
    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets the encoding of SQL migrations.
     *
     * @param encoding The encoding of SQL migrations. (default: UTF-8)
     */
    public void setEncodingAsString(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    /**
     * Sets the default schema managed by MigrateDb. This schema name is case-sensitive. If not specified, but
     * <i>Schemas</i> is, MigrateDb uses the first schema in that list. If that is also not specified, MigrateDb uses
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
    public void setDefaultSchema(String schema) {
        this.defaultSchemaName = schema;
    }

    /**
     * Sets the schemas managed by MigrateDb. These schema names are case-sensitive. If not specified, MigrateDb uses
     * the default schema for the database connection. If <i>defaultSchema</i> is not specified, then the first of this
     * list also acts as default schema.
     * <p>Consequences:</p>
     * <ul>
     * <li>MigrateDb will automatically attempt to create all these schemas, unless they already exist.</li>
     * <li>The schemas will be cleaned in the order of this list.</li>
     * <li>If MigrateDb created them, the schemas themselves will be dropped when cleaning.</li>
     * </ul>
     *
     * @param schemas The schemas managed by MigrateDb. May not be {@code null}. Must contain at least one element.
     */
    public void setSchemas(String... schemas) {
        this.schemaNames = schemas;
    }

    /**
     * Sets the name of the schema history table that will be used by MigrateDb. By default (single-schema mode) the
     * schema history table is placed in the default schema for the connection provided by the datasource. When the
     * <i>migratedb.schemas</i> property is set (multi-schema mode), the schema history table is placed in the first
     * schema of the list.
     *
     * @param table The name of the schema history table that will be used by MigrateDb. (default:
     *              migratedb_schema_history)
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Sets the tablespace where to create the schema history table that will be used by MigrateDb. If not specified,
     * MigrateDb uses the default tablespace for the database connection.This setting is only relevant for databases
     * that do support the notion of tablespaces. Its value is simply ignored for all others.
     *
     * @param tablespace The tablespace where to create the schema history table that will be used by MigrateDb.
     */
    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    /**
     * Sets the target version up to which MigrateDb should consider migrations. Migrations with a higher version number
     * will be ignored. Special values:
     * <ul>
     * <li>{@code current}: Designates the current version of the schema</li>
     * <li>{@code latest}: The latest version of the schema, as defined by the migration with the highest version</li>
     * <li>{@code next}: Next version after currently applied version</li>
     * </ul>
     * Defaults to {@code latest}.
     */
    public void setTarget(MigrationVersion target) {
        this.target = target;
    }

    /**
     * Sets the target version up to which MigrateDb should consider migrations. Migrations with a higher version number
     * will be ignored. Special values:
     * <ul>
     * <li>{@code current}: Designates the current version of the schema</li>
     * <li>{@code latest}: The latest version of the schema, as defined by the migration with the highest version</li>
     * <li>
     *     &lt;version&gt;? (end with a '?'): Instructs MigrateDb not to fail if the target version doesn't exist.
     *     In this case, MigrateDb will go up to but not beyond the specified target
     *     (default: fail if the target version doesn't exist)
     * </li>
     * </ul>
     * Defaults to {@code latest}.
     */
    public void setTargetAsString(String target) {
        if (target.endsWith("?")) {
            setFailOnMissingTarget(false);
            setTarget(MigrationVersion.fromVersion(target.substring(0, target.length() - 1)));
        } else {
            setFailOnMissingTarget(true);
            setTarget(MigrationVersion.fromVersion(target));
        }
    }

    private void setFailOnMissingTarget(boolean failOnMissingTarget) {
        this.failOnMissingTarget = failOnMissingTarget;
    }

    /**
     * Gets the migrations that MigrateDb should consider when migrating or undoing. Leave empty to consider all
     * available migrations. Migrations not in this list will be ignored.
     * <i>MigrateDb Teams only</i>
     */
    public void setCherryPick(MigrationPattern... cherryPick) {
        this.cherryPick = cherryPick;
    }

    /**
     * Gets the migrations that MigrateDb should consider when migrating or undoing. Leave empty to consider all
     * available migrations. Migrations not in this list will be ignored. Values should be the version for versioned
     * migrations (e.g. 1, 2.4, 6.5.3) or the description for repeatable migrations (e.g. Insert_Data, Create_Table)
     * <i>MigrateDb Teams only</i>
     */
    public void setCherryPick(String... cherryPickAsString) {
        this.cherryPick = Arrays.stream(cherryPickAsString)
                                .map(MigrationPattern::new)
                                .toArray(MigrationPattern[]::new);
    }

    /**
     * Sets whether placeholders should be replaced.
     *
     * @param placeholderReplacement Whether placeholders should be replaced. (default: true)
     */
    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    /**
     * Sets the placeholders to replace in SQL migration scripts.
     *
     * @param placeholders The map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Sets the prefix of every placeholder.
     *
     * @param placeholderPrefix The prefix of every placeholder. (default: ${ )
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        if (!StringUtils.hasLength(placeholderPrefix)) {
            throw new MigrateDbException("placeholderPrefix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Sets the prefix of every script placeholder.
     *
     * @param scriptPlaceholderPrefix The prefix of every placeholder. (default: FP__ )
     */
    public void setScriptPlaceholderPrefix(String scriptPlaceholderPrefix) {
        if (!StringUtils.hasLength(scriptPlaceholderPrefix)) {
            throw new MigrateDbException("scriptPlaceholderPrefix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.scriptPlaceholderPrefix = scriptPlaceholderPrefix;
    }

    /**
     * Sets the suffix of every placeholder.
     *
     * @param placeholderSuffix The suffix of every placeholder. (default: } )
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        if (!StringUtils.hasLength(placeholderSuffix)) {
            throw new MigrateDbException("placeholderSuffix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Sets the suffix of every placeholder.
     *
     * @param scriptPlaceholderSuffix The suffix of every placeholder. (default: __ )
     */
    public void setScriptPlaceholderSuffix(String scriptPlaceholderSuffix) {
        if (!StringUtils.hasLength(scriptPlaceholderSuffix)) {
            throw new MigrateDbException("scriptPlaceholderSuffix cannot be empty!", ErrorCode.CONFIGURATION);
        }
        this.scriptPlaceholderSuffix = scriptPlaceholderSuffix;
    }

    /**
     * Sets the file name prefix for sql migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     *
     * @param sqlMigrationPrefix The file name prefix for sql migrations (default: V)
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    /**
     * Sets the file name prefix for baseline migrations. They have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to SB.1__My_description.sql
     *
     * @param baselineMigrationPrefix The file name prefix for baseline migrations (default: B)
     */
    public void setBaselineMigrationPrefix(String baselineMigrationPrefix) {
        this.baselineMigrationPrefix = baselineMigrationPrefix;
    }

    /**
     * Sets the file name prefix for undo SQL migrations. (default: U) Undo SQL migrations are responsible for undoing
     * the effects of the versioned migration with the same version. They have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to U1.1__My_description.sql
     *
     * @param undoSqlMigrationPrefix The file name prefix for undo SQL migrations. (default: U)
     */
    public void setUndoSqlMigrationPrefix(String undoSqlMigrationPrefix) {
        this.undoSqlMigrationPrefix = undoSqlMigrationPrefix;
    }

    /**
     * The manually added Java-based migrations. These are not Java-based migrations discovered through classpath
     * scanning and instantiated by MigrateDb. Instead these are manually added instances of JavaMigration. This is
     * particularly useful when working with a dependency injection container, where you may want the DI container to
     * instantiate the class and wire up its dependencies for you.
     *
     * @param javaMigrations The manually added Java-based migrations. An empty array if none. (default: none)
     */
    public void setJavaMigrations(JavaMigration... javaMigrations) {
        if (javaMigrations == null) {
            throw new MigrateDbException("javaMigrations cannot be null", ErrorCode.CONFIGURATION);
        }
        this.javaMigrations = javaMigrations;
    }

    /**
     * Whether to batch SQL statements when executing them. Batching can save up to 99 percent of network roundtrips by
     * sending up to 100 statements at once over the network to the database, instead of sending each statement
     * individually. This is particularly useful for very large SQL migrations composed of multiple MB or even GB of
     * reference data, as this can dramatically reduce the network overhead. This is supported for INSERT, UPDATE,
     * DELETE, MERGE and UPSERT statements. All other statements are automatically executed without batching.
     *
     * @param batch {@code true} to batch SQL statements. {@code false} to execute them individually instead. (default:
     *              {@code false})
     */
    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    /**
     * Sets the file name prefix for repeatable sql migrations. Repeatable SQL migrations have the following file name
     * structure: prefixSeparatorDESCRIPTIONsuffix, which using the defaults translates to R__My_description.sql
     *
     * @param repeatableSqlMigrationPrefix The file name prefix for repeatable sql migrations (default: R)
     */
    public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
        this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
    }

    /**
     * Sets the file name separator for sql migrations. SQL migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1_1__My_description.sql
     *
     * @param sqlMigrationSeparator The file name separator for sql migrations (default: __)
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
     *
     * @param sqlMigrationSuffixes The file name suffixes for SQL migrations.
     */
    public void setSqlMigrationSuffixes(String... sqlMigrationSuffixes) {
        this.sqlMigrationSuffixes = sqlMigrationSuffixes;
    }

    /**
     * Sets the datasource to use. Must have the necessary privileges to execute DDL.
     *
     * @param dataSource The datasource to use. Must have the necessary privileges to execute DDL.
     */
    public void setDataSource(DataSource dataSource) {
        driver = null;
        url = null;
        user = null;
        password = null;
        this.dataSource = dataSource;
    }

    /**
     * Sets the datasource to use. Must have the necessary privileges to execute DDL. To use a custom ClassLoader,
     * setClassLoader() must be called prior to calling this method.
     *
     * @param url      The JDBC URL of the database.
     * @param user     The user of the database.
     * @param password The password of the database.
     */
    public void setDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.dataSource = new DriverDataSource(classLoader, null, url, user, password, this, databaseTypeRegister);
    }

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, MigrateDb
     * will wait 1 second before attempting to connect again, up to the maximum number of times specified by
     * connectRetries. The interval between retries doubles with each subsequent attempt.
     *
     * @param connectRetries The maximum number of retries (default: 0).
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
     *
     * @param connectRetriesInterval The maximum time between retries in seconds (default: 120).
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
     *
     * @param initSql The SQL statements. (default: {@code null})
     */
    public void setInitSql(String initSql) {
        this.initSql = initSql;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     *
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public void setBaselineVersion(MigrationVersion baselineVersion) {
        this.baselineVersion = baselineVersion;
    }

    /**
     * Sets the version to tag an existing schema with when executing baseline.
     *
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public void setBaselineVersionAsString(String baselineVersion) {
        this.baselineVersion = MigrationVersion.fromVersion(baselineVersion);
    }

    /**
     * Sets the description to tag an existing schema with when executing baseline.
     *
     * @param baselineDescription The description to tag an existing schema with when executing baseline. (default:
     *                            &lt;&lt; MigrateDb Baseline &gt;&gt;)
     */
    public void setBaselineDescription(String baselineDescription) {
        this.baselineDescription = baselineDescription;
    }

    /**
     * Whether to automatically call baseline when migrate is executed against a non-empty schema with no schema history
     * table. This schema will then be baselined with the {@code baselineVersion} before executing the migrations. Only
     * migrations above {@code baselineVersion} will then be applied.
     * <p>
     * This is useful for initial MigrateDb production deployments on projects with an existing DB.
     * <p>
     * Be careful when enabling this as it removes the safety net that ensures MigrateDb does not migrate the wrong
     * database in case of a configuration mistake!
     *
     * @param baselineOnMigrate {@code true} if baseline should be called on migrate for non-empty schemas, {@code
     *                          false} if not. (default: {@code false})
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    /**
     * Allows migrations to be run "out of order". If you already have versions 1 and 3 applied, and now a version 2 is
     * found, it will be applied too instead of being ignored.
     *
     * @param outOfOrder {@code true} if outOfOrder migrations should be applied, {@code false} if not. (default: {@code
     *                   false})
     */
    public void setOutOfOrder(boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
    }

    /**
     * Whether MigrateDb should skip actually executing the contents of the migrations and only update the schema
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
     *
     * @param callbacks The callbacks for lifecycle notifications. (default: none)
     */
    public void setCallbacks(Callback... callbacks) {
        this.callbacks.clear();
        this.callbacks.addAll(Arrays.asList(callbacks));
    }

    /**
     * Set the callbacks for lifecycle notifications.
     *
     * @param callbacks The fully qualified class names of the callbacks for lifecycle notifications. (default: none)
     */
    public void setCallbacksAsClassNames(String... callbacks) {
        this.callbacks.clear();
        this.callbacks.addAll(ClassUtils.instantiateAll(callbacks, classLoader));
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
     * Whether MigrateDb should skip the default callbacks. If true, only custom callbacks are used.
     *
     * @param skipDefaultCallbacks Whether default built-in callbacks should be skipped. <p>(default: false)</p>
     */
    public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
        this.skipDefaultCallbacks = skipDefaultCallbacks;
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     *
     * @param resolvers The custom MigrationResolvers to be used in addition to the built-in ones for resolving
     *                  Migrations to apply. (default: empty list)
     */
    public void setResolvers(MigrationResolver... resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * Sets custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     *
     * @param resolvers The fully qualified class names of the custom MigrationResolvers to be used in addition to the
     *                  built-in ones for resolving Migrations to apply. (default: empty list)
     */
    public void setResolversAsClassNames(String... resolvers) {
        List<MigrationResolver> resolverList = ClassUtils.instantiateAll(resolvers, classLoader);
        setResolvers(resolverList.toArray(new MigrationResolver[resolvers.length]));
    }

    /**
     * Whether MigrateDb should skip the default resolvers. If true, only custom resolvers are used.
     *
     * @param skipDefaultResolvers Whether default built-in resolvers should be skipped. (default: false)
     */
    public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
        this.skipDefaultResolvers = skipDefaultResolvers;
    }

    /**
     * Whether MigrateDb's support for Oracle SQL*Plus commands should be activated.
     * <i>MigrateDb Teams only</i>
     *
     * @param oracleSqlplus {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default:
     *                      {@code false})
     */
    public void setOracleSqlplus(boolean oracleSqlplus) {
        this.oracleSqlplus = oracleSqlplus;
    }

    /**
     * Whether MigrateDb should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus
     * statementit doesn't yet support.
     * <i>MigrateDb Teams only</i>
     *
     * @param oracleSqlplusWarn {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code
     *                          false})
     */
    public void setOracleSqlplusWarn(boolean oracleSqlplusWarn) {
        this.oracleSqlplusWarn = oracleSqlplusWarn;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     * <i>MigrateDb Teams only</i>
     */
    public void setOracleKerberosConfigFile(String oracleKerberosConfigFile) {
        this.oracleKerberosConfigFile = oracleKerberosConfigFile;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     * <i>MigrateDb Teams only</i>
     */
    public void setOracleKerberosCacheFile(String oracleKerberosCacheFile) {
        this.oracleKerberosCacheFile = oracleKerberosCacheFile;
    }

    /**
     * The location of your Oracle wallet, used to automatically sign in to your databases.
     *
     * <i>MigrateDb Teams only</i>
     *
     * @param oracleWalletLocation The path to your Oracle Wallet
     */
    public void setOracleWalletLocation(String oracleWalletLocation) {
        this.oracleWalletLocation = oracleWalletLocation;
    }

    /**
     * Whether MigrateDb should attempt to create the schemas specified in the schemas property.
     *
     * @param createSchemas @{code true} to attempt to create the schemas (default: {@code true})
     */
    public void setShouldCreateSchemas(boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

    /**
     * Does nothing, because this software has been liberated. Merely exists for compatibility.
     */
    public void setLicenseKey(String licenseKey) {
    }

    /**
     * Whether MigrateDb should output a table with the results of queries when executing migrations.
     * <i>MigrateDb Teams only</i>
     *
     * @return {@code true} to output the results table (default: {@code true})
     */
    public void setOutputQueryResults(boolean outputQueryResults) {
        this.outputQueryResults = outputQueryResults;
    }

    /**
     * Properties to pass to the JDBC driver object.
     * <i>MigrateDb Teams only</i>
     */
    public void setJdbcProperties(Map<String, String> jdbcProperties) {
        this.jdbcProperties = jdbcProperties;
    }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void setJavaMigrationClassProvider(ClassProvider<JavaMigration> javaMigrationClassProvider) {
        this.javaMigrationClassProvider = javaMigrationClassProvider;
    }

    public void setLockRetryCount(int lockRetryCount) {
        this.lockRetryCount = lockRetryCount;
    }

    /**
     * Whether to fail if a location specified in the migratedb.locations option doesn't exist
     *
     * @return @{code true} to fail (default: {@code false})
     */
    public void setFailOnMissingLocations(boolean failOnMissingLocations) {
        this.failOnMissingLocations = failOnMissingLocations;
    }

    /**
     * Configure with the same values as this existing configuration.
     */
    public void configure(Configuration configuration) {
        setLogger(configuration.getLogger());
        setBaselineDescription(configuration.getBaselineDescription());
        setBaselineOnMigrate(configuration.isBaselineOnMigrate());
        setBaselineVersion(configuration.getBaselineVersion());
        setCallbacks(configuration.getCallbacks());
        setCleanDisabled(configuration.isCleanDisabled());
        setCleanOnValidationError(configuration.isCleanOnValidationError());
        setDataSource(configuration.getDataSource());
        setConnectRetries(configuration.getConnectRetries());
        setConnectRetriesInterval(configuration.getConnectRetriesInterval());
        setInitSql(configuration.getInitSql());

        setEncoding(configuration.getEncoding());
        setGroup(configuration.isGroup());
        setValidateMigrationNaming(configuration.isValidateMigrationNaming());
        setIgnoreMigrationPatterns(configuration.getIgnoreMigrationPatterns());
        setIgnoreFutureMigrations(configuration.isIgnoreFutureMigrations());
        setIgnoreMissingMigrations(configuration.isIgnoreMissingMigrations());
        setIgnoreIgnoredMigrations(configuration.isIgnoreIgnoredMigrations());
        setIgnorePendingMigrations(configuration.isIgnorePendingMigrations());
        setInstalledBy(configuration.getInstalledBy());
        setJavaMigrations(configuration.getJavaMigrations());
        setLocations(configuration.getLocations());
        setMixed(configuration.isMixed());
        setOutOfOrder(configuration.isOutOfOrder());
        setPlaceholderPrefix(configuration.getPlaceholderPrefix());
        setPlaceholderReplacement(configuration.isPlaceholderReplacement());
        setPlaceholders(configuration.getPlaceholders());
        setPlaceholderSuffix(configuration.getPlaceholderSuffix());
        setScriptPlaceholderPrefix(configuration.getScriptPlaceholderPrefix());
        setScriptPlaceholderSuffix(configuration.getScriptPlaceholderSuffix());
        setRepeatableSqlMigrationPrefix(configuration.getRepeatableSqlMigrationPrefix());
        setResolvers(configuration.getResolvers());
        setDefaultSchema(configuration.getDefaultSchema());
        setSchemas(configuration.getSchemas());
        setSkipDefaultCallbacks(configuration.isSkipDefaultCallbacks());
        setSkipDefaultResolvers(configuration.isSkipDefaultResolvers());
        setSqlMigrationPrefix(configuration.getSqlMigrationPrefix());
        setSqlMigrationSeparator(configuration.getSqlMigrationSeparator());
        setSqlMigrationSuffixes(configuration.getSqlMigrationSuffixes());
        setTable(configuration.getTable());
        setTablespace(configuration.getTablespace());
        setTarget(configuration.getTarget());
        setFailOnMissingTarget(configuration.getFailOnMissingTarget());
        setValidateOnMigrate(configuration.isValidateOnMigrate());
        setResourceProvider(configuration.getResourceProvider());
        setJavaMigrationClassProvider(configuration.getJavaMigrationClassProvider());
        setShouldCreateSchemas(configuration.getCreateSchemas());
        setLockRetryCount(configuration.getLockRetryCount());
        setFailOnMissingLocations(configuration.getFailOnMissingLocations());

        url = configuration.getUrl();
        user = configuration.getUser();
        password = configuration.getPassword();
    }

    /**
     * Configures MigrateDb with these properties. This overwrites any existing configuration. Properties are documented
     * here: https://no-website-yet.org/documentation/configuration/parameters/
     * <p>To use a custom ClassLoader, setClassLoader() must be called prior to calling this method.</p>
     *
     * @param properties Properties used for configuration.
     *
     * @throws MigrateDbException when the configuration failed.
     */
    public void configure(Properties properties) {
        configure(ConfigUtils.propertiesToMap(properties));
    }

    /**
     * Configures MigrateDb with these properties. This overwrites any existing configuration. Properties are documented
     * here: https://no-website-yet.org/documentation/configuration/parameters/
     * <p>To use a custom ClassLoader, it must be passed to the MigrateDb constructor prior to calling this method.</p>
     *
     * @param props Properties used for configuration.
     *
     * @throws MigrateDbException when the configuration failed.
     */
    public void configure(Map<String, String> props) {
        // Make copy to prevent removing elements from the original.
        props = new HashMap<>(props);

        String driverProp = props.remove(ConfigUtils.DRIVER);
        if (driverProp != null) {
            dataSource = null;
            driver = driverProp;
        }
        String urlProp = props.remove(ConfigUtils.URL);
        if (urlProp != null) {
            dataSource = null;
            url = urlProp;
        }
        String userProp = props.remove(ConfigUtils.USER);
        if (userProp != null) {
            dataSource = null;
            user = userProp;
        }
        String passwordProp = props.remove(ConfigUtils.PASSWORD);
        if (passwordProp != null) {
            dataSource = null;
            password = passwordProp;
        }
        Integer connectRetriesProp = removeInteger(props, ConfigUtils.CONNECT_RETRIES);
        if (connectRetriesProp != null) {
            setConnectRetries(connectRetriesProp);
        }
        Integer connectRetriesIntervalProp = removeInteger(props, ConfigUtils.CONNECT_RETRIES_INTERVAL);
        if (connectRetriesIntervalProp != null) {
            setConnectRetriesInterval(connectRetriesIntervalProp);
        }
        String initSqlProp = props.remove(ConfigUtils.INIT_SQL);
        if (initSqlProp != null) {
            setInitSql(initSqlProp);
        }
        String locationsProp = props.remove(ConfigUtils.LOCATIONS);
        if (locationsProp != null) {
            setLocationsAsStrings(StringUtils.tokenizeToStringArray(locationsProp, ","));
        }
        Boolean placeholderReplacementProp = removeBoolean(props, ConfigUtils.PLACEHOLDER_REPLACEMENT);
        if (placeholderReplacementProp != null) {
            setPlaceholderReplacement(placeholderReplacementProp);
        }
        String placeholderPrefixProp = props.remove(ConfigUtils.PLACEHOLDER_PREFIX);
        if (placeholderPrefixProp != null) {
            setPlaceholderPrefix(placeholderPrefixProp);
        }
        String placeholderSuffixProp = props.remove(ConfigUtils.PLACEHOLDER_SUFFIX);
        if (placeholderSuffixProp != null) {
            setPlaceholderSuffix(placeholderSuffixProp);
        }
        String scriptPlaceholderPrefixProp = props.remove(ConfigUtils.SCRIPT_PLACEHOLDER_PREFIX);
        if (scriptPlaceholderPrefixProp != null) {
            setScriptPlaceholderPrefix(scriptPlaceholderPrefixProp);
        }
        String scriptPlaceholderSuffixProp = props.remove(ConfigUtils.SCRIPT_PLACEHOLDER_SUFFIX);
        if (scriptPlaceholderSuffixProp != null) {
            setScriptPlaceholderSuffix(scriptPlaceholderSuffixProp);
        }
        String sqlMigrationPrefixProp = props.remove(ConfigUtils.SQL_MIGRATION_PREFIX);
        if (sqlMigrationPrefixProp != null) {
            setSqlMigrationPrefix(sqlMigrationPrefixProp);
        }
        String undoSqlMigrationPrefixProp = props.remove(ConfigUtils.UNDO_SQL_MIGRATION_PREFIX);
        if (undoSqlMigrationPrefixProp != null) {
            setUndoSqlMigrationPrefix(undoSqlMigrationPrefixProp);
        }
        String baselineMigrationPrefixProp = props.remove(ConfigUtils.BASELINE_MIGRATION_PREFIX);
        if (baselineMigrationPrefixProp != null) {
            setBaselineMigrationPrefix(baselineMigrationPrefixProp);
        }
        String repeatableSqlMigrationPrefixProp = props.remove(ConfigUtils.REPEATABLE_SQL_MIGRATION_PREFIX);
        if (repeatableSqlMigrationPrefixProp != null) {
            setRepeatableSqlMigrationPrefix(repeatableSqlMigrationPrefixProp);
        }
        String sqlMigrationSeparatorProp = props.remove(ConfigUtils.SQL_MIGRATION_SEPARATOR);
        if (sqlMigrationSeparatorProp != null) {
            setSqlMigrationSeparator(sqlMigrationSeparatorProp);
        }
        String sqlMigrationSuffixesProp = props.remove(ConfigUtils.SQL_MIGRATION_SUFFIXES);
        if (sqlMigrationSuffixesProp != null) {
            setSqlMigrationSuffixes(StringUtils.tokenizeToStringArray(sqlMigrationSuffixesProp, ","));
        }
        String encodingProp = props.remove(ConfigUtils.ENCODING);
        if (encodingProp != null) {
            setEncodingAsString(encodingProp);
        }
        String defaultSchemaProp = props.remove(ConfigUtils.DEFAULT_SCHEMA);
        if (defaultSchemaProp != null) {
            setDefaultSchema(defaultSchemaProp);
        }
        String schemasProp = props.remove(ConfigUtils.SCHEMAS);
        if (schemasProp != null) {
            setSchemas(StringUtils.tokenizeToStringArray(schemasProp, ","));
        }
        String tableProp = props.remove(ConfigUtils.TABLE);
        if (tableProp != null) {
            setTable(tableProp);
        }
        String tablespaceProp = props.remove(ConfigUtils.TABLESPACE);
        if (tablespaceProp != null) {
            setTablespace(tablespaceProp);
        }
        Boolean cleanOnValidationErrorProp = removeBoolean(props, ConfigUtils.CLEAN_ON_VALIDATION_ERROR);
        if (cleanOnValidationErrorProp != null) {
            setCleanOnValidationError(cleanOnValidationErrorProp);
        }
        Boolean cleanDisabledProp = removeBoolean(props, ConfigUtils.CLEAN_DISABLED);
        if (cleanDisabledProp != null) {
            setCleanDisabled(cleanDisabledProp);
        }
        Boolean validateOnMigrateProp = removeBoolean(props, ConfigUtils.VALIDATE_ON_MIGRATE);
        if (validateOnMigrateProp != null) {
            setValidateOnMigrate(validateOnMigrateProp);
        }
        String baselineVersionProp = props.remove(ConfigUtils.BASELINE_VERSION);
        if (baselineVersionProp != null) {
            setBaselineVersion(MigrationVersion.fromVersion(baselineVersionProp));
        }
        String baselineDescriptionProp = props.remove(ConfigUtils.BASELINE_DESCRIPTION);
        if (baselineDescriptionProp != null) {
            setBaselineDescription(baselineDescriptionProp);
        }
        Boolean baselineOnMigrateProp = removeBoolean(props, ConfigUtils.BASELINE_ON_MIGRATE);
        if (baselineOnMigrateProp != null) {
            setBaselineOnMigrate(baselineOnMigrateProp);
        }
        Boolean ignoreMissingMigrationsProp = removeBoolean(props, ConfigUtils.IGNORE_MISSING_MIGRATIONS);
        if (ignoreMissingMigrationsProp != null) {
            setIgnoreMissingMigrations(ignoreMissingMigrationsProp);
        }
        Boolean ignoreIgnoredMigrationsProp = removeBoolean(props, ConfigUtils.IGNORE_IGNORED_MIGRATIONS);
        if (ignoreIgnoredMigrationsProp != null) {
            setIgnoreIgnoredMigrations(ignoreIgnoredMigrationsProp);
        }
        Boolean ignorePendingMigrationsProp = removeBoolean(props, ConfigUtils.IGNORE_PENDING_MIGRATIONS);
        if (ignorePendingMigrationsProp != null) {
            setIgnorePendingMigrations(ignorePendingMigrationsProp);
        }
        Boolean ignoreFutureMigrationsProp = removeBoolean(props, ConfigUtils.IGNORE_FUTURE_MIGRATIONS);
        if (ignoreFutureMigrationsProp != null) {
            setIgnoreFutureMigrations(ignoreFutureMigrationsProp);
        }
        Boolean validateMigrationNamingProp = removeBoolean(props, ConfigUtils.VALIDATE_MIGRATION_NAMING);
        if (validateMigrationNamingProp != null) {
            setValidateMigrationNaming(validateMigrationNamingProp);
        }
        String targetProp = props.remove(ConfigUtils.TARGET);
        if (targetProp != null) {
            setTargetAsString(targetProp);
        }
        String cherryPickProp = props.remove(ConfigUtils.CHERRY_PICK);
        if (cherryPickProp != null) {
            setCherryPick(StringUtils.tokenizeToStringArray(cherryPickProp, ","));
        }
        String loggersProp = props.remove(ConfigUtils.LOGGERS);
        if (loggersProp != null) {
            setLogger(StringUtils.tokenizeToStringArray(loggersProp, ","));
        }
        Integer lockRetryCount = removeInteger(props, ConfigUtils.LOCK_RETRY_COUNT);
        if (lockRetryCount != null) {
            setLockRetryCount(lockRetryCount);
        }
        Boolean outOfOrderProp = removeBoolean(props, ConfigUtils.OUT_OF_ORDER);
        if (outOfOrderProp != null) {
            setOutOfOrder(outOfOrderProp);
        }
        Boolean skipExecutingMigrationsProp = removeBoolean(props, ConfigUtils.SKIP_EXECUTING_MIGRATIONS);
        if (skipExecutingMigrationsProp != null) {
            setSkipExecutingMigrations(skipExecutingMigrationsProp);
        }
        Boolean outputQueryResultsProp = removeBoolean(props, ConfigUtils.OUTPUT_QUERY_RESULTS);
        if (outputQueryResultsProp != null) {
            setOutputQueryResults(outputQueryResultsProp);
        }
        String resolversProp = props.remove(ConfigUtils.RESOLVERS);
        if (StringUtils.hasLength(resolversProp)) {
            setResolversAsClassNames(StringUtils.tokenizeToStringArray(resolversProp, ","));
        }
        Boolean skipDefaultResolversProp = removeBoolean(props, ConfigUtils.SKIP_DEFAULT_RESOLVERS);
        if (skipDefaultResolversProp != null) {
            setSkipDefaultResolvers(skipDefaultResolversProp);
        }
        String callbacksProp = props.remove(ConfigUtils.CALLBACKS);
        if (StringUtils.hasLength(callbacksProp)) {
            setCallbacksAsClassNames(StringUtils.tokenizeToStringArray(callbacksProp, ","));
        }
        Boolean skipDefaultCallbacksProp = removeBoolean(props, ConfigUtils.SKIP_DEFAULT_CALLBACKS);
        if (skipDefaultCallbacksProp != null) {
            setSkipDefaultCallbacks(skipDefaultCallbacksProp);
        }
        Map<String, String> placeholdersFromProps = getPropertiesUnderNamespace(props,
                                                                                getPlaceholders(),
                                                                                ConfigUtils.PLACEHOLDERS_PROPERTY_PREFIX);
        setPlaceholders(placeholdersFromProps);
        Boolean mixedProp = removeBoolean(props, ConfigUtils.MIXED);
        if (mixedProp != null) {
            setMixed(mixedProp);
        }
        Boolean groupProp = removeBoolean(props, ConfigUtils.GROUP);
        if (groupProp != null) {
            setGroup(groupProp);
        }
        String installedByProp = props.remove(ConfigUtils.INSTALLED_BY);
        if (installedByProp != null) {
            setInstalledBy(installedByProp);
        }
        String dryRunOutputProp = props.remove(ConfigUtils.DRYRUN_OUTPUT);
        if (dryRunOutputProp != null) {
            setDryRunOutputAsFileName(dryRunOutputProp);
        }
        String errorOverridesProp = props.remove(ConfigUtils.ERROR_OVERRIDES);
        if (errorOverridesProp != null) {
            setErrorOverrides(StringUtils.tokenizeToStringArray(errorOverridesProp, ","));
        }
        Boolean batchProp = removeBoolean(props, ConfigUtils.BATCH);
        if (batchProp != null) {
            setBatch(batchProp);
        }
        Boolean oracleSqlplusProp = removeBoolean(props, ConfigUtils.ORACLE_SQLPLUS);
        if (oracleSqlplusProp != null) {
            setOracleSqlplus(oracleSqlplusProp);
        }
        Boolean oracleSqlplusWarnProp = removeBoolean(props, ConfigUtils.ORACLE_SQLPLUS_WARN);
        if (oracleSqlplusWarnProp != null) {
            setOracleSqlplusWarn(oracleSqlplusWarnProp);
        }
        Boolean createSchemasProp = removeBoolean(props, ConfigUtils.CREATE_SCHEMAS);
        if (createSchemasProp != null) {
            setShouldCreateSchemas(createSchemasProp);
        }
        String oracleKerberosConfigFile = props.remove(ConfigUtils.ORACLE_KERBEROS_CONFIG_FILE);
        if (oracleKerberosConfigFile != null) {
            setOracleKerberosConfigFile(oracleKerberosConfigFile);
        }
        String oracleKerberosCacheFile = props.remove(ConfigUtils.ORACLE_KERBEROS_CACHE_FILE);
        if (oracleKerberosCacheFile != null) {
            setOracleKerberosCacheFile(oracleKerberosCacheFile);
        }
        String oracleWalletLocationProp = props.remove(ConfigUtils.ORACLE_WALLET_LOCATION);
        if (oracleWalletLocationProp != null) {
            setOracleWalletLocation(oracleWalletLocationProp);
        }
        String ignoreMigrationPatternsProp = props.remove(ConfigUtils.IGNORE_MIGRATION_PATTERNS);
        if (ignoreMigrationPatternsProp != null) {
            setIgnoreMigrationPatterns(StringUtils.tokenizeToStringArray(ignoreMigrationPatternsProp, ","));
        }
        Boolean failOnMissingLocationsProp = removeBoolean(props, ConfigUtils.FAIL_ON_MISSING_LOCATIONS);
        if (failOnMissingLocationsProp != null) {
            setFailOnMissingLocations(failOnMissingLocationsProp);
        }

        // Must be done last, so that any driver-specific config has been done at this point.
        if (StringUtils.hasText(url) && (StringUtils.hasText(urlProp) ||
                                         StringUtils.hasText(driverProp) || StringUtils.hasText(userProp) ||
                                         StringUtils.hasText(passwordProp))) {
            Map<String, String> jdbcPropertiesFromProps =
                getPropertiesUnderNamespace(
                    props,
                    getPlaceholders(),
                    ConfigUtils.JDBC_PROPERTIES_PREFIX);

            setDataSource(new DriverDataSource(classLoader,
                                               driver,
                                               url,
                                               user,
                                               password,
                                               this,
                                               jdbcPropertiesFromProps,
                                               databaseTypeRegister));
        }

        ConfigUtils.reportUnrecognisedProperties(props, "migratedb.");
    }

    private Map<String, String> getPropertiesUnderNamespace(Map<String, String> properties, Map<String, String> current,
                                                            String namespace) {
        Iterator<Map.Entry<String, String>> iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String propertyName = entry.getKey();

            if (propertyName.startsWith(namespace) && propertyName.length() > namespace.length()) {
                String placeholderName = propertyName.substring(namespace.length());
                String placeholderValue = entry.getValue();
                current.put(placeholderName, placeholderValue);
                iterator.remove();
            }
        }
        return current;
    }
}
