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
package migratedb.core.internal.configuration;

import static migratedb.core.internal.sqlscript.SqlScriptMetadata.isMultilineBooleanExpression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import migratedb.core.api.ErrorCode;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.logging.Log;
import migratedb.core.internal.util.StringUtils;

public class ConfigUtils {
    private static final Log LOG = Log.getLog(ConfigUtils.class);

    public static final String CONFIG_FILE_NAME = "migratedb.conf";
    public static final String CONFIG_FILES = "migratedb.configFiles";
    public static final String CONFIG_FILE_ENCODING = "migratedb.configFileEncoding";
    public static final String BASELINE_DESCRIPTION = "migratedb.baselineDescription";
    public static final String BASELINE_ON_MIGRATE = "migratedb.baselineOnMigrate";
    public static final String BASELINE_VERSION = "migratedb.baselineVersion";
    public static final String BATCH = "migratedb.batch";
    public static final String CALLBACKS = "migratedb.callbacks";
    public static final String CLEAN_DISABLED = "migratedb.cleanDisabled";
    public static final String CLEAN_ON_VALIDATION_ERROR = "migratedb.cleanOnValidationError";
    public static final String CONNECT_RETRIES = "migratedb.connectRetries";
    public static final String CONNECT_RETRIES_INTERVAL = "migratedb.connectRetriesInterval";
    public static final String DEFAULT_SCHEMA = "migratedb.defaultSchema";
    public static final String DRIVER = "migratedb.driver";
    public static final String DRYRUN_OUTPUT = "migratedb.dryRunOutput";
    public static final String ENCODING = "migratedb.encoding";
    public static final String ERROR_OVERRIDES = "migratedb.errorOverrides";
    public static final String GROUP = "migratedb.group";
    public static final String IGNORE_FUTURE_MIGRATIONS = "migratedb.ignoreFutureMigrations";
    public static final String IGNORE_MISSING_MIGRATIONS = "migratedb.ignoreMissingMigrations";
    public static final String IGNORE_IGNORED_MIGRATIONS = "migratedb.ignoreIgnoredMigrations";
    public static final String IGNORE_PENDING_MIGRATIONS = "migratedb.ignorePendingMigrations";
    public static final String IGNORE_MIGRATION_PATTERNS = "migratedb.ignoreMigrationPatterns";
    public static final String INIT_SQL = "migratedb.initSql";
    public static final String INSTALLED_BY = "migratedb.installedBy";
    public static final String LOCATIONS = "migratedb.locations";
    public static final String MIXED = "migratedb.mixed";
    public static final String OUT_OF_ORDER = "migratedb.outOfOrder";
    public static final String SKIP_EXECUTING_MIGRATIONS = "migratedb.skipExecutingMigrations";
    public static final String OUTPUT_QUERY_RESULTS = "migratedb.outputQueryResults";
    public static final String PASSWORD = "migratedb.password";
    public static final String PLACEHOLDER_PREFIX = "migratedb.placeholderPrefix";
    public static final String PLACEHOLDER_REPLACEMENT = "migratedb.placeholderReplacement";
    public static final String PLACEHOLDER_SUFFIX = "migratedb.placeholderSuffix";
    public static final String SCRIPT_PLACEHOLDER_PREFIX = "migratedb.scriptPlaceholderPrefix";
    public static final String SCRIPT_PLACEHOLDER_SUFFIX = "migratedb.scriptPlaceholderSuffix";
    public static final String PLACEHOLDERS_PROPERTY_PREFIX = "migratedb.placeholders.";
    public static final String LOCK_RETRY_COUNT = "migratedb.lockRetryCount";
    public static final String JDBC_PROPERTIES_PREFIX = "migratedb.jdbcProperties.";
    public static final String REPEATABLE_SQL_MIGRATION_PREFIX = "migratedb.repeatableSqlMigrationPrefix";
    public static final String RESOLVERS = "migratedb.resolvers";
    public static final String SCHEMAS = "migratedb.schemas";
    public static final String SKIP_DEFAULT_CALLBACKS = "migratedb.skipDefaultCallbacks";
    public static final String SKIP_DEFAULT_RESOLVERS = "migratedb.skipDefaultResolvers";
    public static final String SQL_MIGRATION_PREFIX = "migratedb.sqlMigrationPrefix";
    public static final String SQL_MIGRATION_SEPARATOR = "migratedb.sqlMigrationSeparator";
    public static final String SQL_MIGRATION_SUFFIXES = "migratedb.sqlMigrationSuffixes";
    public static final String STATE_SCRIPT_PREFIX = "migratedb.stateScriptPrefix";
    public static final String TABLE = "migratedb.table";
    public static final String TABLESPACE = "migratedb.tablespace";
    public static final String TARGET = "migratedb.target";
    public static final String CHERRY_PICK = "migratedb.cherryPick";
    public static final String UNDO_SQL_MIGRATION_PREFIX = "migratedb.undoSqlMigrationPrefix";
    public static final String URL = "migratedb.url";
    public static final String USER = "migratedb.user";
    public static final String VALIDATE_ON_MIGRATE = "migratedb.validateOnMigrate";
    public static final String VALIDATE_MIGRATION_NAMING = "migratedb.validateMigrationNaming";
    public static final String CREATE_SCHEMAS = "migratedb.createSchemas";
    public static final String FAIL_ON_MISSING_LOCATIONS = "migratedb.failOnMissingLocations";
    public static final String LOGGERS = "migratedb.loggers";

    // Oracle-specific
    public static final String ORACLE_SQLPLUS = "migratedb.oracle.sqlplus";
    public static final String ORACLE_SQLPLUS_WARN = "migratedb.oracle.sqlplusWarn";
    public static final String ORACLE_KERBEROS_CONFIG_FILE = "migratedb.oracle.kerberosConfigFile";
    public static final String ORACLE_KERBEROS_CACHE_FILE = "migratedb.oracle.kerberosCacheFile";
    public static final String ORACLE_WALLET_LOCATION = "migratedb.oracle.walletLocation";

    // Command-line specific
    public static final String JAR_DIRS = "migratedb.jarDirs";

    private ConfigUtils() {
    }

    /**
     * Reads the configuration from a Reader.
     *
     * @return The properties from the configuration file. An empty Map if none.
     *
     * @throws MigrateDbException When the configuration could not be read.
     */
    public static Map<String, String> loadConfiguration(Reader reader) {
        try {
            return tryLoadConfig(reader instanceof BufferedReader ? (BufferedReader) reader
                                                                  : new BufferedReader(reader));
        } catch (IOException e) {
            throw new MigrateDbException("Unable to read config", e);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static Map<String, String> tryLoadConfig(BufferedReader reader)
    throws IOException {
        String[] lines = reader.lines().toArray(String[]::new);

        StringBuilder confBuilder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String replacedLine = lines[i].trim().replace("\\", "\\\\");

            // if the line ends in a \\, then it may be a multiline property
            if (replacedLine.endsWith("\\\\")) {
                // if we aren't the last line
                if (i < lines.length - 1) {
                    // look ahead to see if the next line is a blank line, a property, or another multiline
                    String nextLine = lines[i + 1];
                    boolean restoreMultilineDelimiter = false;
                    if (nextLine.isEmpty()) {
                        // blank line
                    } else if (nextLine.contains("=")) {
                        if (isMultilineBooleanExpression(nextLine)) {
                            // next line is an extension of a boolean expression
                            restoreMultilineDelimiter = true;
                        }
                        // next line is a property
                    } else {
                        // line with content, this was a multiline property
                        restoreMultilineDelimiter = true;
                    }

                    if (restoreMultilineDelimiter) {
                        // it's a multiline property, so restore the original single slash
                        replacedLine = replacedLine.substring(0, replacedLine.length() - 2) + "\\";
                    }
                }
            }

            confBuilder.append(replacedLine).append("\n");
        }
        String contents = confBuilder.toString();

        Properties properties = new Properties();
        properties.load(new StringReader(contents));
        return ConfigUtils.propertiesToMap(properties);
    }

    /**
     * Converts {@code properties} into a map.
     */
    public static Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> props = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return props;
    }

    /**
     * Puts this property in the config if it has been set in any of these values.
     *
     * @param config The config.
     * @param key    The property name.
     * @param values The values to try. The first non-null value will be set.
     */
    public static void putIfSet(Map<String, String> config, String key, Object... values) {
        for (Object value : values) {
            if (value != null) {
                config.put(key, value.toString());
                return;
            }
        }
    }

    /**
     * Puts this property in the config if it has been set in any of these values.
     *
     * @param config The config.
     * @param key    The property name.
     * @param values The values to try. The first non-null value will be set.
     */
    public static void putArrayIfSet(Map<String, String> config, String key, String[]... values) {
        for (String[] value : values) {
            if (value != null) {
                config.put(key, StringUtils.arrayToCommaDelimitedString(value));
                return;
            }
        }
    }

    /**
     * @param config The config.
     * @param key    The property name.
     *
     * @return The property value as a boolean if it exists, otherwise {@code null}.
     *
     * @throws MigrateDbException when the property value is not a valid boolean.
     */
    public static Boolean removeBoolean(Map<String, String> config, String key) {
        String value = config.remove(key);
        if (value == null) {
            return null;
        }
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new MigrateDbException("Invalid value for " + key + " (should be either true or false): " + value,
                                         ErrorCode.CONFIGURATION);
        }
        return Boolean.valueOf(value);
    }

    /**
     * @param config The config.
     * @param key    The property name.
     *
     * @return The property value as an integer if it exists, otherwise {@code null}.
     *
     * @throws MigrateDbException When the property value is not a valid integer.
     */
    public static Integer removeInteger(Map<String, String> config, String key) {
        String value = config.remove(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new MigrateDbException("Invalid value for " + key + " (should be an integer): " + value,
                                         ErrorCode.CONFIGURATION);
        }
    }

    /**
     * Reports all remaining keys in {@code config} that start with {@code prefix} as unrecognised by throwing an
     * exception. Does nothing if {@code config} is empty or none of its keys start with {@code prefix}.
     *
     * @param prefix The expected prefix for MigrateDb configuration parameters. {@code null} if none.
     */
    public static void reportUnrecognisedProperties(Map<String, String> config, String prefix) {
        ArrayList<String> unknownMigrateDbProperties = new ArrayList<>();
        for (String key : config.keySet()) {
            if (prefix == null || key.startsWith(prefix)) {
                unknownMigrateDbProperties.add(key);
            }
        }

        if (!unknownMigrateDbProperties.isEmpty()) {
            String property = (unknownMigrateDbProperties.size() == 1) ? "property" : "properties";
            String message = String.format("Unknown configuration %s: %s",
                                           property,
                                           StringUtils.arrayToCommaDelimitedString(unknownMigrateDbProperties.toArray()));
            throw new MigrateDbException(message, ErrorCode.CONFIGURATION);
        }
    }
}
