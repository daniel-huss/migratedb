/*
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

package migratedb.v1.core.api.configuration;

import migratedb.v1.core.api.Location;
import migratedb.v1.core.api.TargetVersion;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.logging.LogSystems;
import migratedb.v1.core.api.pattern.ValidatePattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Metadata on the string-based configuration properties supported by
 * {@link DefaultConfiguration#configure(Properties)}.
 */
public final class PropertyNames {
    @Info(typeHint = String.class)
    public static final String BASELINE_DESCRIPTION = "migratedb.baselineDescription";

    @Info(typeHint = Boolean.class)
    public static final String BASELINE_ON_MIGRATE = "migratedb.baselineOnMigrate";

    @Info(typeHint = Version.class)
    public static final String BASELINE_VERSION = "migratedb.baselineVersion";

    @Info(typeHint = Class.class, commaSeparated = true)
    public static final String CALLBACKS = "migratedb.callbacks";

    @Info(typeHint = Integer.class)
    public static final String CONNECT_RETRIES = "migratedb.connectRetries";

    @Info(typeHint = Integer.class)
    public static final String CONNECT_RETRIES_INTERVAL = "migratedb.connectRetriesInterval";

    @Info(typeHint = String.class)
    public static final String DEFAULT_SCHEMA = "migratedb.defaultSchema";

    @Info(typeHint = Charset.class)
    public static final String ENCODING = "migratedb.encoding";

    @Info(typeHint = Boolean.class)
    public static final String GROUP = "migratedb.group";

    @Info(typeHint = Boolean.class)
    public static final String IGNORE_FUTURE_MIGRATIONS = "migratedb.ignoreFutureMigrations";

    @Info(typeHint = Boolean.class)
    public static final String IGNORE_MISSING_MIGRATIONS = "migratedb.ignoreMissingMigrations";

    @Info(typeHint = Boolean.class)
    public static final String IGNORE_IGNORED_MIGRATIONS = "migratedb.ignoreIgnoredMigrations";

    @Info(typeHint = Boolean.class)
    public static final String IGNORE_PENDING_MIGRATIONS = "migratedb.ignorePendingMigrations";

    @Info(typeHint = ValidatePattern.class)
    public static final String IGNORE_MIGRATION_PATTERNS = "migratedb.ignoreMigrationPatterns";

    @Info(typeHint = String.class)
    public static final String INIT_SQL = "migratedb.initSql";

    @Info(typeHint = String.class)
    public static final String INSTALLED_BY = "migratedb.installedBy";

    @Info(typeHint = Location.class, commaSeparated = true)
    public static final String LOCATIONS = "migratedb.locations";

    @Info(typeHint = Boolean.class)
    public static final String MIXED = "migratedb.mixed";

    @Info(typeHint = Boolean.class)
    public static final String OUT_OF_ORDER = "migratedb.outOfOrder";

    @Info(typeHint = Boolean.class)
    public static final String SKIP_EXECUTING_MIGRATIONS = "migratedb.skipExecutingMigrations";

    @Info(typeHint = Boolean.class)
    public static final String OUTPUT_QUERY_RESULTS = "migratedb.outputQueryResults";

    @Info(typeHint = String.class)
    public static final String PLACEHOLDER_PREFIX = "migratedb.placeholderPrefix";

    @Info(typeHint = Boolean.class)
    public static final String PLACEHOLDER_REPLACEMENT = "migratedb.placeholderReplacement";

    @Info(typeHint = String.class)
    public static final String PLACEHOLDER_SUFFIX = "migratedb.placeholderSuffix";

    @Info(typeHint = String.class)
    public static final String SCRIPT_PLACEHOLDER_PREFIX = "migratedb.scriptPlaceholderPrefix";

    @Info(typeHint = String.class)
    public static final String SCRIPT_PLACEHOLDER_SUFFIX = "migratedb.scriptPlaceholderSuffix";

    @Info(typeHint = String.class, isPrefix = true)
    public static final String PLACEHOLDERS_PROPERTY_PREFIX = "migratedb.placeholders.";

    @Info(typeHint = Integer.class)
    public static final String LOCK_RETRY_COUNT = "migratedb.lockRetryCount";

    @Info(typeHint = String.class)
    public static final String REPEATABLE_SQL_MIGRATION_PREFIX = "migratedb.repeatableSqlMigrationPrefix";

    @Info(typeHint = Class.class, commaSeparated = true)
    public static final String RESOLVERS = "migratedb.resolvers";

    @Info(typeHint = String.class, commaSeparated = true)
    public static final String SCHEMAS = "migratedb.schemas";

    @Info(typeHint = Boolean.class)
    public static final String SKIP_DEFAULT_CALLBACKS = "migratedb.skipDefaultCallbacks";

    @Info(typeHint = Boolean.class)
    public static final String SKIP_DEFAULT_RESOLVERS = "migratedb.skipDefaultResolvers";

    @Info(typeHint = String.class)
    public static final String SQL_MIGRATION_PREFIX = "migratedb.sqlMigrationPrefix";

    @Info(typeHint = String.class)
    public static final String SQL_MIGRATION_SEPARATOR = "migratedb.sqlMigrationSeparator";

    @Info(typeHint = String.class)
    public static final String SQL_MIGRATION_SUFFIXES = "migratedb.sqlMigrationSuffixes";

    @Info(typeHint = String.class)
    public static final String BASELINE_MIGRATION_PREFIX = "migratedb.baselineMigrationPrefix";

    @Info(typeHint = String.class)
    public static final String TABLE = "migratedb.table";

    @Info(typeHint = String.class)
    public static final String OLD_TABLE = "migratedb.oldTable";

    @Info(typeHint = String.class)
    public static final String TABLESPACE = "migratedb.tablespace";

    @Info(typeHint = TargetVersion.class)
    public static final String TARGET = "migratedb.target";

    @Info(typeHint = Version.class, commaSeparated = true)
    public static final String CHERRY_PICK = "migratedb.cherryPick";

    @Info(typeHint = Boolean.class)
    public static final String VALIDATE_ON_MIGRATE = "migratedb.validateOnMigrate";

    @Info(typeHint = Boolean.class)
    public static final String VALIDATE_MIGRATION_NAMING = "migratedb.validateMigrationNaming";

    @Info(typeHint = Boolean.class)
    public static final String CREATE_SCHEMAS = "migratedb.createSchemas";

    @Info(typeHint = Boolean.class)
    public static final String FAIL_ON_MISSING_LOCATIONS = "migratedb.failOnMissingLocations";

    @Info(typeHint = Class.class, acceptsStringConstantsIn = LogSystems.class, commaSeparated = true)
    public static final String LOGGER = "migratedb.logger";

    @Info(typeHint = Boolean.class)
    public static final String LIBERATE_ON_MIGRATE = "migratedb.createSchemas";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Info {
        Class<?> typeHint();

        boolean commaSeparated() default false;

        boolean isPrefix() default false;

        Class<?>[] acceptsStringConstantsIn() default {};
    }
}
