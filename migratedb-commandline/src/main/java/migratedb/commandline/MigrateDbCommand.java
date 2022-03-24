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
package migratedb.commandline;

import static java.nio.charset.StandardCharsets.UTF_8;
import static migratedb.commandline.CommandLineConfigKey.CONFIG_FILES;
import static migratedb.commandline.CommandLineConfigKey.CONFIG_FILE_ENCODING;
import static migratedb.commandline.CommandLineConfigKey.JAR_DIRS;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_DESCRIPTION;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_ON_MIGRATE;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_VERSION;
import static migratedb.core.api.configuration.PropertyNames.BATCH;
import static migratedb.core.api.configuration.PropertyNames.CALLBACKS;
import static migratedb.core.api.configuration.PropertyNames.CHERRY_PICK;
import static migratedb.core.api.configuration.PropertyNames.CLEAN_DISABLED;
import static migratedb.core.api.configuration.PropertyNames.CLEAN_ON_VALIDATION_ERROR;
import static migratedb.core.api.configuration.PropertyNames.CONNECT_RETRIES;
import static migratedb.core.api.configuration.PropertyNames.CONNECT_RETRIES_INTERVAL;
import static migratedb.core.api.configuration.PropertyNames.CREATE_SCHEMAS;
import static migratedb.core.api.configuration.PropertyNames.DEFAULT_SCHEMA;
import static migratedb.core.api.configuration.PropertyNames.DRIVER;
import static migratedb.core.api.configuration.PropertyNames.DRYRUN_OUTPUT;
import static migratedb.core.api.configuration.PropertyNames.ENCODING;
import static migratedb.core.api.configuration.PropertyNames.ERROR_OVERRIDES;
import static migratedb.core.api.configuration.PropertyNames.FAIL_ON_MISSING_LOCATIONS;
import static migratedb.core.api.configuration.PropertyNames.GROUP;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_FUTURE_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_IGNORED_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_MIGRATION_PATTERNS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_MISSING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_PENDING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.INIT_SQL;
import static migratedb.core.api.configuration.PropertyNames.INSTALLED_BY;
import static migratedb.core.api.configuration.PropertyNames.JDBC_PROPERTIES_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.LOCATIONS;
import static migratedb.core.api.configuration.PropertyNames.LOCK_RETRY_COUNT;
import static migratedb.core.api.configuration.PropertyNames.LOGGER;
import static migratedb.core.api.configuration.PropertyNames.MIXED;
import static migratedb.core.api.configuration.PropertyNames.OUTPUT_QUERY_RESULTS;
import static migratedb.core.api.configuration.PropertyNames.OUT_OF_ORDER;
import static migratedb.core.api.configuration.PropertyNames.PASSWORD;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDERS_PROPERTY_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_REPLACEMENT;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_SUFFIX;
import static migratedb.core.api.configuration.PropertyNames.REPEATABLE_SQL_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.RESOLVERS;
import static migratedb.core.api.configuration.PropertyNames.SCHEMAS;
import static migratedb.core.api.configuration.PropertyNames.SCRIPT_PLACEHOLDER_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.SCRIPT_PLACEHOLDER_SUFFIX;
import static migratedb.core.api.configuration.PropertyNames.SKIP_DEFAULT_CALLBACKS;
import static migratedb.core.api.configuration.PropertyNames.SKIP_DEFAULT_RESOLVERS;
import static migratedb.core.api.configuration.PropertyNames.SKIP_EXECUTING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_SEPARATOR;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_SUFFIXES;
import static migratedb.core.api.configuration.PropertyNames.TABLE;
import static migratedb.core.api.configuration.PropertyNames.TABLESPACE;
import static migratedb.core.api.configuration.PropertyNames.TARGET;
import static migratedb.core.api.configuration.PropertyNames.URL;
import static migratedb.core.api.configuration.PropertyNames.USER;
import static migratedb.core.api.configuration.PropertyNames.VALIDATE_MIGRATION_NAMING;
import static migratedb.core.api.configuration.PropertyNames.VALIDATE_ON_MIGRATE;
import static migratedb.core.internal.configuration.ConfigUtils.loadConfiguration;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_KERBEROS_CACHE_FILE;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_KERBEROS_CONFIG_FILE;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_SQLPLUS;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_SQLPLUS_WARN;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_WALLET_LOCATION;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import migratedb.core.MigrateDb;
import migratedb.core.api.DatabaseTypeRegister;
import migratedb.core.api.Location;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.api.configuration.PropertyNames;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.CompositeResult;
import migratedb.core.api.output.ErrorOutput;
import migratedb.core.api.output.OperationResult;
import migratedb.core.internal.database.DatabaseTypeRegisterImpl;
import migratedb.core.internal.info.BuildInfo;
import migratedb.core.internal.info.MigrationInfoDumper;
import migratedb.core.internal.schemahistory.SchemaHistory;
import migratedb.core.internal.util.ClassUtils;
import migratedb.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

class MigrateDbCommand {
    private static final Log LOG = Log.getLog(MigrateDbCommand.class);

    public static final String CONFIG_FILE_NAME = "migratedb.conf";

    private final Arguments arguments;
    private final Console console;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final @Nullable InputStream stdin;
    private final Map<String, String> environment;
    private final DatabaseTypeRegister databaseTypeRegister;

    MigrateDbCommand(Arguments arguments,
                     @Nullable Console console,
                     PrintStream stdout,
                     PrintStream stderr,
                     @Nullable InputStream stdin,
                     Map<String, String> environment) {
        this.arguments = arguments;
        this.console = console;
        this.stdout = stdout;
        this.stderr = stderr;
        this.stdin = stdin;
        this.environment = environment;
        this.databaseTypeRegister = new DatabaseTypeRegisterImpl();
    }

    int run() throws Exception {
        try {
            arguments.validate();

            if (arguments.shouldPrintVersionAndExit()) {
                printVersion();
                return ExitCode.OK;
            }

            if (arguments.hasOperation("help") || arguments.shouldPrintUsage()) {
                printUsage();
                return ExitCode.OK;
            }

            Map<String, String> envVars = environmentVariablesToPropertyMap();

            Map<String, String> config = new HashMap<>();
            initializeDefaults(config);
            loadConfigurationFromConfigFiles(config, envVars);

            if (arguments.isWorkingDirectorySet()) {
                makeRelativeLocationsBasedOnWorkingDirectory(config);
            }

            config.putAll(envVars);
            config = overrideConfiguration(config, arguments.getConfiguration());

            ClassLoader classLoader = ClassUtils.defaultClassLoader();
            List<File> jarFiles = new ArrayList<>();
            jarFiles.addAll(getJdbcDriverJarFiles());
            jarFiles.addAll(getJavaMigrationJarFiles(config));
            if (!jarFiles.isEmpty()) {
                classLoader = addJarsOrDirectoriesToClasspath(classLoader, jarFiles);
            }

            if (!arguments.shouldSuppressPrompt()) {
                promptForCredentialsIfMissing(config);
            }

            dumpConfiguration(config);
            filterProperties(config);

            MigrateDb migratedb = MigrateDb.configure(classLoader).configuration(config).load();

            OperationResult result;
            if (arguments.getOperations().size() == 1) {
                String operation = arguments.getOperations().get(0);
                result = executeOperation(migratedb, operation);
            } else {
                var compositeResult = new CompositeResult();
                for (String operation : arguments.getOperations()) {
                    OperationResult individualResult = executeOperation(migratedb, operation);
                    compositeResult.individualResults.add(individualResult);
                }
                result = compositeResult;
            }

            if (arguments.shouldOutputJson()) {
                printJson(result);
            }
            return ExitCode.OK;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (arguments.shouldOutputJson()) {
                ErrorOutput errorOutput = ErrorOutput.fromException(e);
                printJson(errorOutput);
            } else {
                if (arguments.getLogLevel() == LogLevel.DEBUG) {
                    LOG.error("Unexpected error", e);
                } else {
                    LOG.error(getMessagesFromException(e));
                }
            }
            return ExitCode.EXCEPTION;
        }
    }

    /**
     * Converts MigrateDB-specific environment variables to their matching properties.
     *
     * @return The properties corresponding to the environment variables.
     */
    private Map<String, String> environmentVariablesToPropertyMap() {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String convertedKey = convertKey(entry.getKey());
            if (convertedKey != null) {
                // Known environment variable
                result.put(convertKey(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static @Nullable String convertKey(String key) {
        if ("MIGRATEDB_BASELINE_DESCRIPTION".equals(key)) {
            return BASELINE_DESCRIPTION;
        }
        if ("MIGRATEDB_BASELINE_ON_MIGRATE".equals(key)) {
            return BASELINE_ON_MIGRATE;
        }
        if ("MIGRATEDB_BASELINE_VERSION".equals(key)) {
            return BASELINE_VERSION;
        }
        if ("MIGRATEDB_BATCH".equals(key)) {
            return BATCH;
        }
        if ("MIGRATEDB_CALLBACKS".equals(key)) {
            return CALLBACKS;
        }
        if ("MIGRATEDB_CLEAN_DISABLED".equals(key)) {
            return CLEAN_DISABLED;
        }
        if ("MIGRATEDB_CLEAN_ON_VALIDATION_ERROR".equals(key)) {
            return CLEAN_ON_VALIDATION_ERROR;
        }
        if ("MIGRATEDB_CONFIG_FILE_ENCODING".equals(key)) {
            return CONFIG_FILE_ENCODING;
        }
        if ("MIGRATEDB_CONFIG_FILES".equals(key)) {
            return CONFIG_FILES;
        }
        if ("MIGRATEDB_CONNECT_RETRIES".equals(key)) {
            return CONNECT_RETRIES;
        }
        if ("MIGRATEDB_CONNECT_RETRIES_INTERVAL".equals(key)) {
            return CONNECT_RETRIES_INTERVAL;
        }
        if ("MIGRATEDB_DEFAULT_SCHEMA".equals(key)) {
            return DEFAULT_SCHEMA;
        }
        if ("MIGRATEDB_DRIVER".equals(key)) {
            return DRIVER;
        }
        if ("MIGRATEDB_DRYRUN_OUTPUT".equals(key)) {
            return DRYRUN_OUTPUT;
        }
        if ("MIGRATEDB_ENCODING".equals(key)) {
            return ENCODING;
        }
        if ("MIGRATEDB_ERROR_OVERRIDES".equals(key)) {
            return ERROR_OVERRIDES;
        }
        if ("MIGRATEDB_GROUP".equals(key)) {
            return GROUP;
        }
        if ("MIGRATEDB_IGNORE_FUTURE_MIGRATIONS".equals(key)) {
            return IGNORE_FUTURE_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_MISSING_MIGRATIONS".equals(key)) {
            return IGNORE_MISSING_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_IGNORED_MIGRATIONS".equals(key)) {
            return IGNORE_IGNORED_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_PENDING_MIGRATIONS".equals(key)) {
            return IGNORE_PENDING_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_MIGRATION_PATTERNS".equals(key)) {
            return IGNORE_MIGRATION_PATTERNS;
        }
        if ("MIGRATEDB_INIT_SQL".equals(key)) {
            return INIT_SQL;
        }
        if ("MIGRATEDB_INSTALLED_BY".equals(key)) {
            return INSTALLED_BY;
        }
        if ("MIGRATEDB_LOCATIONS".equals(key)) {
            return LOCATIONS;
        }
        if ("MIGRATEDB_MIXED".equals(key)) {
            return MIXED;
        }
        if ("MIGRATEDB_OUT_OF_ORDER".equals(key)) {
            return OUT_OF_ORDER;
        }
        if ("MIGRATEDB_SKIP_EXECUTING_MIGRATIONS".equals(key)) {
            return SKIP_EXECUTING_MIGRATIONS;
        }
        if ("MIGRATEDB_OUTPUT_QUERY_RESULTS".equals(key)) {
            return OUTPUT_QUERY_RESULTS;
        }
        if ("MIGRATEDB_PASSWORD".equals(key)) {
            return PASSWORD;
        }
        if ("MIGRATEDB_LOCK_RETRY_COUNT".equals(key)) {
            return LOCK_RETRY_COUNT;
        }
        if ("MIGRATEDB_PLACEHOLDER_PREFIX".equals(key)) {
            return PLACEHOLDER_PREFIX;
        }
        if ("MIGRATEDB_PLACEHOLDER_REPLACEMENT".equals(key)) {
            return PLACEHOLDER_REPLACEMENT;
        }
        if ("MIGRATEDB_PLACEHOLDER_SUFFIX".equals(key)) {
            return PLACEHOLDER_SUFFIX;
        }
        if ("MIGRATEDB_SCRIPT_PLACEHOLDER_PREFIX".equals(key)) {
            return SCRIPT_PLACEHOLDER_PREFIX;
        }
        if ("MIGRATEDB_SCRIPT_PLACEHOLDER_SUFFIX".equals(key)) {
            return SCRIPT_PLACEHOLDER_SUFFIX;
        }
        if (key.matches("MIGRATEDB_PLACEHOLDERS_.+")) {
            return PLACEHOLDERS_PROPERTY_PREFIX +
                   key.substring("MIGRATEDB_PLACEHOLDERS_".length()).toLowerCase(Locale.ENGLISH);
        }

        if (key.matches("MIGRATEDB_JDBC_PROPERTIES_.+")) {
            return JDBC_PROPERTIES_PREFIX + key.substring("MIGRATEDB_JDBC_PROPERTIES_".length());
        }

        if ("MIGRATEDB_REPEATABLE_SQL_MIGRATION_PREFIX".equals(key)) {
            return REPEATABLE_SQL_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_RESOLVERS".equals(key)) {
            return RESOLVERS;
        }
        if ("MIGRATEDB_SCHEMAS".equals(key)) {
            return SCHEMAS;
        }
        if ("MIGRATEDB_SKIP_DEFAULT_CALLBACKS".equals(key)) {
            return SKIP_DEFAULT_CALLBACKS;
        }
        if ("MIGRATEDB_SKIP_DEFAULT_RESOLVERS".equals(key)) {
            return SKIP_DEFAULT_RESOLVERS;
        }
        if ("MIGRATEDB_SQL_MIGRATION_PREFIX".equals(key)) {
            return SQL_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_SQL_MIGRATION_SEPARATOR".equals(key)) {
            return SQL_MIGRATION_SEPARATOR;
        }
        if ("MIGRATEDB_SQL_MIGRATION_SUFFIXES".equals(key)) {
            return SQL_MIGRATION_SUFFIXES;
        }
        if ("MIGRATEDB_BASELINE_MIGRATION_PREFIX".equals(key)) {
            return BASELINE_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_TABLE".equals(key)) {
            return TABLE;
        }
        if ("MIGRATEDB_TABLESPACE".equals(key)) {
            return TABLESPACE;
        }
        if ("MIGRATEDB_TARGET".equals(key)) {
            return TARGET;
        }
        if ("MIGRATEDB_CHERRY_PICK".equals(key)) {
            return CHERRY_PICK;
        }
        if ("MIGRATEDB_LOGGERS".equals(key)) {
            return LOGGER;
        }
        if ("MIGRATEDB_URL".equals(key)) {
            return URL;
        }
        if ("MIGRATEDB_USER".equals(key)) {
            return USER;
        }
        if ("MIGRATEDB_VALIDATE_ON_MIGRATE".equals(key)) {
            return VALIDATE_ON_MIGRATE;
        }
        if ("MIGRATEDB_VALIDATE_MIGRATION_NAMING".equals(key)) {
            return VALIDATE_MIGRATION_NAMING;
        }
        if ("MIGRATEDB_CREATE_SCHEMAS".equals(key)) {
            return CREATE_SCHEMAS;
        }
        if ("MIGRATEDB_FAIL_ON_MISSING_LOCATIONS".equals(key)) {
            return FAIL_ON_MISSING_LOCATIONS;
        }

        // Oracle-specific
        if ("MIGRATEDB_ORACLE_SQLPLUS".equals(key)) {
            return ORACLE_SQLPLUS;
        }
        if ("MIGRATEDB_ORACLE_SQLPLUS_WARN".equals(key)) {
            return ORACLE_SQLPLUS_WARN;
        }
        if ("MIGRATEDB_ORACLE_KERBEROS_CONFIG_FILE".equals(key)) {
            return ORACLE_KERBEROS_CONFIG_FILE;
        }
        if ("MIGRATEDB_ORACLE_KERBEROS_CACHE_FILE".equals(key)) {
            return ORACLE_KERBEROS_CACHE_FILE;
        }
        if ("MIGRATEDB_ORACLE_WALLET_LOCATION".equals(key)) {
            return ORACLE_WALLET_LOCATION;
        }

        // Command-line specific
        if ("MIGRATEDB_JAR_DIRS".equals(key)) {
            return JAR_DIRS;
        }
        return null;
    }

    private String expandEnvironmentVariables(String value) {
        Pattern pattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
        Matcher matcher = pattern.matcher(value);
        String expandedValue = value;

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = environment.getOrDefault(variableName, "");

            LOG.debug("Expanding environment variable in config: " + variableName + " -> " + variableValue);
            expandedValue = expandedValue.replaceAll(Pattern.quote(matcher.group(0)),
                                                     Matcher.quoteReplacement(variableValue));
        }

        return expandedValue;
    }

    /**
     * Adds these jars or directories to the classpath.
     *
     * @param classLoader The current ClassLoader.
     * @param jarFiles    The jars or directories to add.
     *
     * @return The new ClassLoader containing the additional jar or directory.
     */
    private ClassLoader addJarsOrDirectoriesToClasspath(ClassLoader classLoader, List<File> jarFiles) {
        List<URL> urls = new ArrayList<>();
        for (File jarFile : jarFiles) {
            try {
                urls.add(jarFile.toURI().toURL());
            } catch (Exception e) {
                throw new MigrateDbException("Unable to load " + jarFile.getPath(), e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), classLoader);
    }

    private void dumpConfiguration(Map<String, String> config) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using configuration:");
            for (Map.Entry<String, String> entry : new TreeMap<>(config).entrySet()) {
                String value = entry.getValue();

                switch (entry.getKey()) {
                    // Mask the password. Ex.: T0pS3cr3t -> *********
                    case PropertyNames.PASSWORD:
                        value = StringUtils.trimOrPad("", value.length(), '*');
                        break;
                    // Mask any password in the URL
                    case PropertyNames.URL:
                        value = databaseTypeRegister.redactJdbcUrl(value);
                        break;
                }

                LOG.debug(entry.getKey() + " -> " + value);
            }
        }
    }

    /**
     * Load configuration files from the default locations: $installationDir$/conf/migratedb.conf
     * $user.home$/migratedb.conf $workingDirectory$/migratedb.conf
     * <p>
     * The configuration files must be encoded with UTF-8.
     *
     * @throws MigrateDbException When the configuration failed.
     */
    private FluentConfiguration loadDefaultConfigurationFiles() {
        String installationPath = ClassUtils.guessLocationOnDisk(MigrateDbCommand.class);
        if (installationPath == null) {
            throw new MigrateDbException("Cannot determine installation path");
        }
        File installationDir = new File(installationPath).getParentFile();

        Map<String, String> configMap = loadDefaultConfigurationFiles(installationDir, "UTF-8");

        return new FluentConfiguration().configuration(configMap);
    }

    /**
     * Load configuration files from the default locations: $installationDir$/conf/migratedb.conf
     * $user.home$/migratedb.conf $workingDirectory$/migratedb.conf
     *
     * @param encoding The conf file encoding.
     *
     * @throws MigrateDbException When the configuration failed.
     */
    private Map<String, String> loadDefaultConfigurationFiles(File installationDir, String encoding) {
        Map<String, String> configMap = new HashMap<>();
        configMap.putAll(loadConfigurationFile(new File(
            installationDir.getAbsolutePath() + "/conf/" + CONFIG_FILE_NAME), encoding, false));
        configMap.putAll(loadConfigurationFile(new File(
            System.getProperty("user.home") + "/" + CONFIG_FILE_NAME), encoding, false));
        configMap.putAll(loadConfigurationFile(new File(CONFIG_FILE_NAME), encoding, false));

        return configMap;
    }

    /**
     * Loads the configuration from this configuration file.
     *
     * @param configFile    The configuration file to load.
     * @param encoding      The encoding of the configuration file.
     * @param failIfMissing Whether to fail if the file is missing.
     *
     * @return The properties from the configuration file. An empty Map if none.
     *
     * @throws MigrateDbException When the configuration file could not be loaded.
     */
    private Map<String, String> loadConfigurationFile(File configFile, String encoding, boolean failIfMissing)
    throws MigrateDbException {
        String errorMessage = "Unable to load config file: " + configFile.getAbsolutePath();

        if ("-".equals(configFile.getName())) {
            return loadConfigurationFromStdin();
        } else if (!configFile.isFile() || !configFile.canRead()) {
            if (!failIfMissing) {
                LOG.debug(errorMessage);
                return new HashMap<>();
            }
            throw new MigrateDbException(errorMessage);
        }

        LOG.debug("Loading config file: " + configFile.getAbsolutePath());

        try {
            return loadConfiguration(new InputStreamReader(new FileInputStream(configFile), encoding));
        } catch (IOException | MigrateDbException e) {
            throw new MigrateDbException(errorMessage, e);
        }
    }

    private Map<String, String> loadConfigurationFromStdin() {
        var config = new HashMap<String, String>();
        try {
            if (stdin != null) {
                try (var bufferedReader = new BufferedReader(new InputStreamReader(stdin, UTF_8))) {
                    LOG.debug("Attempting to load configuration from standard input");
                    var configFromStdin = loadConfiguration(bufferedReader);
                    if (configFromStdin.isEmpty()) {
                        LOG.warn("Configuration from standard input is empty");
                    }
                    config.putAll(configFromStdin);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not load configuration from standard input " + e.getMessage());
        }
        return config;
    }

    private void makeRelativeLocationsBasedOnWorkingDirectory(Map<String, String> config) {
        String[] locations = config.get(PropertyNames.LOCATIONS).split(",");
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].startsWith(Location.FileSystemLocation.PREFIX)) {
                String newLocation = locations[i].substring(Location.FileSystemLocation.PREFIX.length());
                File file = new File(newLocation);
                if (!file.isAbsolute()) {
                    file = new File(arguments.getWorkingDirectory(), newLocation);
                }
                locations[i] = Location.FileSystemLocation.PREFIX + file.getAbsolutePath();
            }
        }

        config.put(PropertyNames.LOCATIONS, StringUtils.arrayToCommaDelimitedString(locations));
    }

    private Map<String, String> overrideConfiguration(Map<String, String> existingConfiguration,
                                                      Map<String, String> newConfiguration) {
        Map<String, String> combinedConfiguration = new HashMap<>();

        combinedConfiguration.putAll(existingConfiguration);
        combinedConfiguration.putAll(newConfiguration);

        return combinedConfiguration;
    }

    private String getMessagesFromException(Throwable e) {
        StringBuilder condensedMessages = new StringBuilder();
        String preamble = "";
        while (e != null) {
            if (e instanceof MigrateDbException) {
                condensedMessages.append(preamble).append(e.getMessage());
            } else {
                condensedMessages.append(preamble).append(e);
            }
            preamble = "\nCaused by: ";
            e = e.getCause();
        }
        return condensedMessages.toString();
    }

    @SuppressWarnings("IfCanBeSwitch")
    private @Nullable OperationResult executeOperation(MigrateDb migratedb, String operation) {
        OperationResult result = null;
        if ("clean".equals(operation)) {
            result = migratedb.clean();
        } else if ("baseline".equals(operation)) {
            result = migratedb.baseline();
        } else if ("migrate".equals(operation)) {
            result = migratedb.migrate();
        } else if ("validate".equals(operation)) {
            if (arguments.shouldOutputJson()) {
                result = migratedb.validateWithResult();
            } else {
                migratedb.validate();
            }
        } else if ("info".equals(operation)) {
            var info = migratedb.info();
            var current = info.current();
            var currentSchemaVersion = current == null ? null : current.getVersion();

            LOG.info("Schema version: " + (currentSchemaVersion == null ? SchemaHistory.EMPTY_SCHEMA_DESCRIPTION
                                                                        : currentSchemaVersion));
            LOG.info("");

            result = info.getInfoResult();
            MigrationInfo[] infos = info.all();

            LOG.info(MigrationInfoDumper.dumpToAsciiTable(infos));
        } else if ("repair".equals(operation)) {
            result = migratedb.repair();
        } else {
            LOG.error("Invalid operation: " + operation);
            printUsage();
            throw new MigrateDbException("Invalid operation");
        }

        return result;
    }

    private void printJson(@Nullable OperationResult object) throws IOException {
        String json = convertObjectToJsonString(object);
        byte[] bytes = json.getBytes(UTF_8);
        if (arguments.isOutputFileSet()) {
            Path path = Paths.get(arguments.getOutputFile());
            try {
                Files.write(path,
                            bytes,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new MigrateDbException("Could not write to output file " + arguments.getOutputFile(),
                                             e);
            }
        }

        stdout.write(bytes);
    }

    private String convertObjectToJsonString(@Nullable Object object) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
        return gson.toJson(object);
    }

    private void initializeDefaults(Map<String, String> config) {
        // To maintain override order, return extension value first if present
        String workingDirectory = arguments.isWorkingDirectorySet() ? arguments.getWorkingDirectory()
                                                                    : getInstallationDir();
        config.put(PropertyNames.LOCATIONS, "filesystem:" + new File(workingDirectory, "sql").getAbsolutePath());
        config.put(JAR_DIRS, new File(workingDirectory, "jars").getAbsolutePath());
    }

    /**
     * Filters the properties to remove the MigrateDb Commandline-specific ones.
     */
    private void filterProperties(Map<String, String> config) {
        config.remove(JAR_DIRS);
        config.remove(CONFIG_FILES);
        config.remove(CONFIG_FILE_ENCODING);
    }

    private void printVersion() {
        LOG.info("MigrateDB version " + BuildInfo.VERSION);
        LOG.debug("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        LOG.debug(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " +
                  System.getProperty("os.arch") + "\n");
    }

    private void printUsage() {
        LOG.info("Usage");
        LOG.info("=====");
        LOG.info("");
        LOG.info("migratedb [options] command");
        LOG.info("");
        LOG.info("By default, the configuration will be read from conf/migratedb.conf.");
        LOG.info("Options passed from the command-line override the configuration.");
        LOG.info("");
        LOG.info("Commands");
        LOG.info("--------");
        LOG.info("migrate  : Migrates the database");
        LOG.info("clean    : Drops all objects in the configured schemas");
        LOG.info("info     : Prints the information about applied, current and pending migrations");
        LOG.info("validate : Validates the applied migrations against the ones on the classpath");
        LOG.info("baseline : Baselines an existing database at the baselineVersion");
        LOG.info("repair   : Repairs the schema history table");
        LOG.info("");
        LOG.info("Options (Format: -key=value)");
        LOG.info("-------");
        LOG.info("driver                       : Fully qualified classname of the JDBC driver");
        LOG.info("url                          : JDBC url to use to connect to the database");
        LOG.info("user                         : User to use to connect to the database");
        LOG.info("password                     : Password to use to connect to the database");
        LOG.info("connectRetries               : Maximum number of retries when attempting to connect to the database");
        LOG.info("initSql                      : SQL statements to run to initialize a new database connection");
        LOG.info("schemas                      : Comma-separated list of the schemas managed by MigrateDb");
        LOG.info("table                        : Name of MigrateDB's schema history table");
        LOG.info("locations                    : Classpath locations to scan recursively for migrations");
        LOG.info(
            "failOnMissingLocations       : Whether to fail if a location specified in the migratedb.locations option" +
            " doesn't exist");
        LOG.info("resolvers                    : Comma-separated list of custom MigrationResolvers");
        LOG.info("skipDefaultResolvers         : Skips default resolvers (jdbc, sql and Spring-jdbc)");
        LOG.info("sqlMigrationPrefix           : File name prefix for versioned SQL migrations");
        LOG.info("repeatableSqlMigrationPrefix : File name prefix for repeatable SQL migrations");
        LOG.info("sqlMigrationSeparator        : File name separator for SQL migrations");
        LOG.info("sqlMigrationSuffixes         : Comma-separated list of file name suffixes for SQL migrations");
        LOG.info("mixed                        : Allow mixing transactional and non-transactional statements");
        LOG.info("encoding                     : Encoding of SQL migrations");
        LOG.info("placeholderReplacement       : Whether placeholders should be replaced");
        LOG.info("placeholders                 : Placeholders to replace in sql migrations");
        LOG.info("placeholderPrefix            : Prefix of every placeholder");
        LOG.info("placeholderSuffix            : Suffix of every placeholder");
        LOG.info("scriptPlaceholderPrefix      : Prefix of every script placeholder");
        LOG.info("scriptPlaceholderSuffix      : Suffix of every script placeholder");
        LOG.info("lockRetryCount               : The maximum number of retries when trying to obtain a lock");
        LOG.info("jdbcProperties               : Properties to pass to the JDBC driver object");
        LOG.info("installedBy                  : Username that will be recorded in the schema history table");
        LOG.info("target                       : Target version up to which MigrateDB should use migrations");
        LOG.info("outOfOrder                   : Allows migrations to be run \"out of order\"");
        LOG.info(
            "callbacks                    : Comma-separated list of MigrateDbCallback classes, or locations to scan " +
            "for MigrateDbCallback classes");
        LOG.info("skipDefaultCallbacks         : Skips default callbacks (sql)");
        LOG.info("validateOnMigrate            : Validate when running migrate");
        LOG.info("validateMigrationNaming      : Validate file names of SQL migrations (including callbacks)");
        LOG.info("ignoreMissingMigrations      : Allow missing migrations when validating");
        LOG.info("ignoreIgnoredMigrations      : Allow ignored migrations when validating");
        LOG.info("ignorePendingMigrations      : Allow pending migrations when validating");
        LOG.info("ignoreFutureMigrations       : Allow future migrations when validating");
        LOG.info("cleanOnValidationError       : Automatically clean on a validation error");
        LOG.info("cleanDisabled                : Whether to disable clean");
        LOG.info("baselineVersion              : Version to tag schema with when executing baseline");
        LOG.info("baselineDescription          : Description to tag schema with when executing baseline");
        LOG.info("baselineOnMigrate            : Baseline on migrate against uninitialized non-empty schema");
        LOG.info("configFiles                  : Comma-separated list of config files to use");
        LOG.info("configFileEncoding           : Encoding to use when loading the config files");
        LOG.info("jarDirs                      : Comma-separated list of dirs for Jdbc drivers & Java migrations");
        LOG.info(
            "createSchemas                : Whether MigrateDB should attempt to create the schemas specified in the " +
            "schemas property");
        LOG.info("outputFile                   : Send output to the specified file alongside the console");
        LOG.info("outputType                   : Serialise the output in the given format, Values: json");
        LOG.info("");
        LOG.info("Flags");
        LOG.info("-----");
        LOG.info("-X              : Print debug output");
        LOG.info("-q              : Suppress all output, except for errors and warnings");
        LOG.info("-n              : Suppress prompting for a user and password");
        LOG.info("--version, -v   : Print the MigrateDB version and exit");
        LOG.info("--help, -h, -?  : Print this usage info and exit");
        LOG.info("");
        LOG.info("Example");
        LOG.info("-------");
        LOG.info("migratedb -user=myuser -password=s3cr3t -url=jdbc:h2:mem -placeholders.abc=def migrate");
    }

    private List<File> getJdbcDriverJarFiles() {
        File driversDir = new File(getInstallationDir(), "drivers");
        File[] files = driversDir.listFiles((dir, name) -> name.endsWith(".jar"));

        // see javadoc of listFiles(): null if given path is not a real directory
        if (files == null) {
            LOG.debug("Directory for JDBC Drivers not found: " + driversDir.getAbsolutePath());
            return Collections.emptyList();
        }

        return Arrays.asList(files);
    }

    private List<File> getJavaMigrationJarFiles(Map<String, String> config) throws IOException {
        String jarDirs = config.get(JAR_DIRS);
        if (!StringUtils.hasLength(jarDirs)) {
            return Collections.emptyList();
        }

        jarDirs = jarDirs.replace(File.pathSeparator, ",");
        String[] dirs = StringUtils.tokenizeToStringArray(jarDirs, ",");

        List<File> jarFiles = new ArrayList<>();
        for (String dirName : dirs) {
            File dir = new File(dirName);
            File[] files = dir.listFiles((dir1, name) -> name.endsWith(".jar"));

            // see javadoc of listFiles(): null if given path is not a real directory
            if (files == null) {
                LOG.error("Directory for Java Migrations not found: " + dirName);
                throw new FileNotFoundException(dirName);
            }

            jarFiles.addAll(Arrays.asList(files));
        }

        return jarFiles;
    }

    private void loadConfigurationFromConfigFiles(Map<String, String> config,
                                                  Map<String, String> envVars) {
        String encoding = determineConfigurationFileEncoding(arguments, envVars);
        File installationDir = new File(getInstallationDir());

        config.putAll(loadDefaultConfigurationFiles(installationDir, encoding));

        for (File configFile : determineConfigFilesFromArgs(arguments, envVars)) {
            config.putAll(loadConfigurationFile(configFile, encoding, true));
        }
    }

    /**
     * If no user or password has been provided, prompt for it. If you want to avoid the prompt, pass in an empty user
     * or password.
     *
     * @param config The properties object to load to configuration into.
     */
    private void promptForCredentialsIfMissing(Map<String, String> config) {
        if (console == null) {
            // We are running in an automated build. Prompting is not possible.
            return;
        }

        if (!config.containsKey(PropertyNames.URL)) {
            // URL is not set. We are doomed for failure anyway.
            return;
        }

        String url = config.get(PropertyNames.URL);
        if (!config.containsKey(PropertyNames.USER) && needsUser(url,
                                                                 config.getOrDefault(PropertyNames.PASSWORD, null))) {
            config.put(PropertyNames.USER, console.readLine("Database user: "));
        }

        if (!config.containsKey(PropertyNames.PASSWORD) && needsPassword(url, config.get(PropertyNames.USER))) {
            char[] password = console.readPassword("Database password: ");
            config.put(PropertyNames.PASSWORD, password == null ? "" : String.valueOf(password));
        }
    }

    /**
     * Detect whether the JDBC URL specifies a known authentication mechanism that does not need a username.
     */
    private boolean needsUser(String url, String password) {
        DatabaseType databaseType = databaseTypeRegister.getDatabaseTypeForUrl(url);
        return databaseType.detectUserRequiredByUrl(url);
    }

    /**
     * Detect whether the JDBC URL specifies a known authentication mechanism that does not need a password.
     */
    private boolean needsPassword(String url, String username) {
        DatabaseType databaseType = databaseTypeRegister.getDatabaseTypeForUrl(url);
        return databaseType.detectPasswordRequiredByUrl(url);
    }

    private List<File> determineConfigFilesFromArgs(Arguments arguments,
                                                    Map<String, String> envVars) {
        List<File> configFiles = new ArrayList<>();

        String workingDirectory =
            arguments.isWorkingDirectorySet() ? arguments.getWorkingDirectory() : null;

        if (envVars.containsKey(CONFIG_FILES)) {
            for (String file : StringUtils.tokenizeToStringArray(envVars.get(CONFIG_FILES), ",")) {
                configFiles.add(new File(workingDirectory, file));
            }
            return configFiles;
        }

        for (String file : arguments.getConfigFiles()) {
            configFiles.add(new File(workingDirectory, file));
        }

        return configFiles;
    }

    private static String getInstallationDir() {
        String path = ClassUtils.guessLocationOnDisk(MigrateDbCommand.class);
        if (path == null) {
            throw new MigrateDbException("Cannot determine installation directory");
        }
        return new File(path) // jar file
                              .getParentFile() // edition dir
                              .getParentFile() // lib dir
                              .getParentFile() // installation dir
                              .getAbsolutePath();
    }

    /**
     * @return The encoding. (default: UTF-8)
     */
    private String determineConfigurationFileEncoding(Arguments arguments,
                                                      Map<String, String> envVars) {
        if (envVars.containsKey(CONFIG_FILE_ENCODING)) {
            return envVars.get(CONFIG_FILE_ENCODING);
        }

        if (arguments.isConfigFileEncodingSet()) {
            return arguments.getConfigFileEncoding();
        }

        return "UTF-8";
    }
}
