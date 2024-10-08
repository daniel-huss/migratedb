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
package migratedb.v1.commandline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import migratedb.v1.core.MigrateDb;
import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.Location;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.MigrateDbExtension;
import migratedb.v1.core.api.configuration.DefaultConfiguration;
import migratedb.v1.core.api.configuration.PropertyNames;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.output.CompositeResult;
import migratedb.v1.core.api.output.OperationResult;
import migratedb.v1.core.internal.info.BuildInfo;
import migratedb.v1.core.internal.info.MigrationInfoDumper;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;
import migratedb.v1.core.internal.util.ClassUtils;
import migratedb.v1.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static migratedb.v1.core.internal.configuration.ConfigUtils.loadConfiguration;

class MigrateDbCommand {
    private static final Log LOG = Log.getLog(MigrateDbCommand.class);

    public static final String CONFIG_FILE_NAME = "migratedb.conf";

    private final Arguments arguments;
    private final Console console;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final @Nullable InputStream stdin;
    private final Map<String, String> environment;
    private final FileSystem fileSystem;
    private final Path installationDir;
    private final Path driversDir;
    private final Path configDir;

    private @MonotonicNonNull Supplier<MigrateDb> migrateDb;

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
        this.fileSystem = arguments.getFileSystem();
        this.installationDir = fileSystem.getPath(arguments.getInstallationDirectory());
        this.driversDir = installationDir.resolve("drivers");
        this.configDir = installationDir.resolve("conf");
    }

    int run() throws Exception {
        String currentOperation = null;
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

            OperationResult result;
            if (arguments.getOperations().size() == 1) {
                currentOperation = arguments.getOperations().get(0);
                result = executeOperation(currentOperation);
            } else {
                var compositeResult = new CompositeResult();
                for (String operation : arguments.getOperations()) {
                    currentOperation = operation;
                    OperationResult individualResult = executeOperation(operation);
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
                printJson(unhandledExceptionErrorOutput(currentOperation, e));
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

    private OperationResult unhandledExceptionErrorOutput(@Nullable String currentOperation, Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof MigrateDbException) {
            MigrateDbException migratedbException = (MigrateDbException) exception;
            return new ErrorOutput(
                    migratedbException.getErrorCode(),
                    message == null ? "Error occurred" : message,
                    arguments.getLogLevel() == LogLevel.DEBUG ? getStackTrace(exception) : null,
                    currentOperation);
        } else {
            return new ErrorOutput(
                    ErrorCode.FAULT,
                    message == null ? "Fault occurred" : message,
                    getStackTrace(exception),
                    currentOperation);
        }
    }

    private static String getStackTrace(Exception exception) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8);
        exception.printStackTrace(printStream);
        return output.toString(StandardCharsets.UTF_8);
    }

    private MigrateDb createMigrateDb() throws IOException {
        Map<String, String> envVars = environmentVariablesToPropertyMap();

        Map<String, String> configProps = new HashMap<>();
        initializeDefaults(configProps);
        loadConfigurationFromConfigFiles(configProps, envVars);

        configProps.putAll(envVars);
        configProps = overrideConfiguration(configProps, arguments.getConfiguration());

        var url = configProps.get(CommandLineConfigKey.URL);
        var user = configProps.get(CommandLineConfigKey.USER);
        var password = configProps.get(CommandLineConfigKey.PASSWORD);
        var driverClassName = configProps.get(CommandLineConfigKey.DRIVER);

        if (arguments.isWorkingDirectorySet()) {
            makeRelativeLocationsBasedOnWorkingDirectory(configProps);
        }

        var classLoader = createClassLoader(configProps);
        var spiExtensions = ServiceLoader.load(MigrateDbExtension.class, classLoader)
                                         .stream()
                                         .map(ServiceLoader.Provider::get)
                                         .collect(Collectors.toList());

        var driverSupport = getDriverSupport(url, classLoader);

        if (!arguments.shouldSuppressPrompt()) {
            promptForCredentialsIfMissing(configProps, driverSupport);
        }

        dumpConfiguration(configProps, driverSupport);
        var commandLineProperties = filterProperties(configProps);

        var dataSource = new DriverSupportDataSource(classLoader,
                                                     driverClassName,
                                                     url,
                                                     user,
                                                     password,
                                                     CommandLineConfigKey.getJdbcProperties(commandLineProperties),
                                                     driverSupport);

        var configuration = new DefaultConfiguration(classLoader);
        configuration.configure(configProps);
        configuration.useExtensions(spiExtensions);
        configuration.setDataSource(dataSource);

        return new MigrateDb(configuration);
    }

    private ClassLoader createClassLoader(Map<String, String> configProps) throws IOException {
        var classLoader = ClassUtils.defaultClassLoader();
        var jarFiles = new ArrayList<Path>();
        jarFiles.addAll(getJdbcDriverJarFiles());
        jarFiles.addAll(getJavaMigrationJarFiles(configProps));
        if (!jarFiles.isEmpty()) {
            classLoader = addJarsOrDirectoriesToClasspath(classLoader, jarFiles);
        }
        return classLoader;
    }

    /**
     * @return The corresponding driver support, if the JDBC URL is supported. {@code null}, otherwise.
     */
    private DriverSupport getDriverSupport(String url, ClassLoader classLoader) {
        var registry = new DriverSupportRegistry();
        ServiceLoader.load(DriverSupport.class, classLoader).forEach(registry::register);
        return registry.getDriverSupportForUrl(url);
    }

    /**
     * Converts MigrateDB-specific environment variables to their matching properties.
     *
     * @return The properties corresponding to the environment variables.
     */
    private Map<String, String> environmentVariablesToPropertyMap() {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String convertedKey = EnvironmentMapper.convertKey(entry.getKey());
            if (convertedKey != null) {
                // Known environment variable
                result.put(EnvironmentMapper.convertKey(entry.getKey()), expandEnvironmentVariables(entry.getValue()));
            }
        }
        return result;
    }

    private String expandEnvironmentVariables(String value) {
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)}");
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
     * @return The new ClassLoader containing the additional jar or directory.
     */
    private ClassLoader addJarsOrDirectoriesToClasspath(ClassLoader classLoader, List<Path> jarFiles) {
        List<URL> urls = new ArrayList<>();
        for (var jarFile : jarFiles) {
            try {
                urls.add(jarFile.toUri().toURL());
            } catch (MalformedURLException | RuntimeException e) {
                throw new MigrateDbException("Unable to load " + jarFile, e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), classLoader);
    }

    private void dumpConfiguration(Map<String, String> config, DriverSupport driverSupport) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using configuration:");
            for (Map.Entry<String, String> entry : new TreeMap<>(config).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.toLowerCase(Locale.ROOT).endsWith("password")) {
                    value = StringUtils.trimOrPad("", value.length(), '*');
                } else if (key.equals(CommandLineConfigKey.URL)) {
                    value = driverSupport.redactJdbcUrl(value);

                }
                LOG.debug(key + " -> " + value);
            }
        }
    }

    /**
     * Load configuration files from the default locations: $installationDir$/conf/migratedb.conf
     * $user.home$/migratedb.conf $workingDirectory$/migratedb.conf
     *
     * @param encoding The conf file encoding.
     * @throws MigrateDbException When the configuration failed.
     */
    private Map<String, String> loadDefaultConfigurationFiles(String encoding) {
        Map<String, String> configMap = new HashMap<>();
        configMap.putAll(loadConfigurationFile(configDir.resolve(CONFIG_FILE_NAME),
                                               encoding,
                                               false));
        configMap.putAll(loadConfigurationFile(fileSystem.getPath(System.getProperty("user.home"), CONFIG_FILE_NAME),
                                               encoding,
                                               false));
        configMap.putAll(loadConfigurationFile(fileSystem.getPath(CONFIG_FILE_NAME), encoding, false));
        return configMap;
    }

    /**
     * Loads the configuration from this configuration file.
     *
     * @param configFile    The configuration file to load.
     * @param encoding      The encoding of the configuration file.
     * @param failIfMissing Whether to fail if the file is missing.
     * @return The properties from the configuration file. An empty Map if none.
     * @throws MigrateDbException When the configuration file could not be loaded.
     */
    private Map<String, String> loadConfigurationFile(Path configFile,
                                                      String encoding,
                                                      boolean failIfMissing) throws MigrateDbException {
        String errorMessage = "Unable to load config file: " + configFile.toAbsolutePath();

        if ("-".equals(configFile.getFileName().toString())) {
            return loadConfigurationFromStdin();
        } else if (!Files.isRegularFile(configFile) || !Files.isReadable(configFile)) {
            if (!failIfMissing) {
                LOG.debug(errorMessage);
                return new HashMap<>();
            }
            throw new MigrateDbException(errorMessage);
        }

        LOG.debug("Loading config file: " + configFile.toAbsolutePath());

        try {
            return loadConfiguration(new InputStreamReader(Files.newInputStream(configFile), encoding));
        } catch (IOException | MigrateDbException e) {
            throw new MigrateDbException(errorMessage, e);
        }
    }

    private Map<String, String> loadConfigurationFromStdin() {
        var config = new HashMap<String, String>();
        try {
            if (stdin != null) {
                try (var reader = waitForStdin()) {
                    LOG.debug("Attempting to load configuration from standard input");
                    var configFromStdin = loadConfiguration(reader);
                    if (configFromStdin.isEmpty()) {
                        LOG.warn("Configuration from standard input is empty");
                    }
                    config.putAll(configFromStdin);
                }
            }
        } catch (RuntimeException | IOException | ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("Could not load configuration from standard input " + e.getMessage());
        }
        return config;
    }

    /**
     * In some scenarios, attempting to read from STDIN may block forever when there is no data. In this case we want
     * the application to terminate instead of waiting forever.
     */
    private Reader waitForStdin() throws ExecutionException, InterruptedException {
        var exec = Executors.newSingleThreadExecutor((r) -> {
            var thread = new Thread(r);
            thread.setName("Waiting for STDIN");
            thread.setDaemon(true);
            return thread;
        });
        var stream = stdin;
        assert stream != null;
        var future = exec.submit(() -> {
            if (stream.available() == 0) {
                var markSupport = new BufferedInputStream(stream);
                markSupport.mark(2);
                if (markSupport.read() == -1) { // read() blocks (maybe forever)
                    throw new MigrateDbException("No input provided");
                }
                markSupport.reset();
                return new InputStreamReader(markSupport, UTF_8);
            }
            return new InputStreamReader(stream, UTF_8);
        });
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new MigrateDbException("Timeout while waiting for STDIN");
        }
    }

    private void makeRelativeLocationsBasedOnWorkingDirectory(Map<String, String> config) {
        String[] locations = config.get(PropertyNames.LOCATIONS).split(",");
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].startsWith(Location.FileSystemLocation.PREFIX)) {
                String newLocation = locations[i].substring(Location.FileSystemLocation.PREFIX.length());
                var file = fileSystem.getPath(newLocation);
                if (!file.isAbsolute()) {
                    file = fileSystem.getPath(arguments.getWorkingDirectory(), newLocation);
                }
                locations[i] = Location.FileSystemLocation.PREFIX + file.toAbsolutePath();
            }
        }

        config.put(PropertyNames.LOCATIONS, String.join(",", locations));
    }

    private Map<String, String> overrideConfiguration(Map<String, String> existingConfiguration,
                                                      Map<String, String> newConfiguration) {
        Map<String, String> combinedConfiguration = new HashMap<>(existingConfiguration);
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
    private @Nullable OperationResult executeOperation(String operation) throws IOException {
        OperationResult result = null;
        if ("download-drivers".equals(operation)) {
            result = new DownloadDriversCommand(
                    parseDriverDefinitions(configDir.resolve("drivers.yaml")),
                    driversDir,
                    arguments.getDriverNames())
                    .run();
            // Re-create with new available drivers
            migrateDb = lazy(this::createMigrateDb);
            return result;
        }
        if (migrateDb == null) {
            migrateDb = lazy(this::createMigrateDb);
        }
        if ("baseline".equals(operation)) {
            result = migrateDb.get().baseline();
        } else if ("migrate".equals(operation)) {
            result = migrateDb.get().migrate();
        } else if ("validate".equals(operation)) {
            if (arguments.shouldOutputJson()) {
                result = migrateDb.get().validateWithResult();
            } else {
                migrateDb.get().validate();
            }
        } else if ("info".equals(operation)) {
            var info = migrateDb.get().info();
            var current = info.current();
            var currentSchemaVersion = current == null ? null : current.getVersion();

            if (!arguments.shouldOutputJson()) {
                stdout.println(
                        "Schema version: " + (currentSchemaVersion == null ? SchemaHistory.EMPTY_SCHEMA_DESCRIPTION
                                : currentSchemaVersion));
                stdout.println();
                stdout.println(MigrationInfoDumper.dumpToAsciiTable(info.all()));
            }

            result = info.getInfoResult();
        } else if ("repair".equals(operation)) {
            result = migrateDb.get().repair();
        } else if ("liberate".equals(operation)) {
            result = migrateDb.get().liberate();
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
            Path path = fileSystem.getPath(arguments.getOutputFile());
            try {
                Files.write(path,
                            bytes,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new MigrateDbException("Could not write to output file " + arguments.getOutputFile(), e);
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
                : installationDir.toString();
        config.put(PropertyNames.LOCATIONS,
                   "filesystem:" + fileSystem.getPath(workingDirectory, "sql").toAbsolutePath());
        config.put(CommandLineConfigKey.JAR_DIRS, fileSystem.getPath(workingDirectory, "jars").toAbsolutePath().toString());
    }

    /**
     * Filters the properties to remove the MigrateDB command line specific ones.
     *
     * @return The entries that have been removed.
     */
    private Map<String, String> filterProperties(Map<String, String> config) {
        var result = new HashMap<String, String>();
        for (var key : List.of(
                CommandLineConfigKey.JAR_DIRS,
                CommandLineConfigKey.CONFIG_FILES,
                CommandLineConfigKey.CONFIG_FILE_ENCODING,
                CommandLineConfigKey.URL,
                CommandLineConfigKey.USER,
                CommandLineConfigKey.PASSWORD,
                CommandLineConfigKey.DRIVER
        )) {
            String removed = config.remove(key);
            if (removed != null) {
                result.put(key, removed);
            }
        }
        for (var entries = config.entrySet().iterator(); entries.hasNext(); ) {
            var entry = entries.next();
            if (entry.getKey().startsWith(CommandLineConfigKey.JDBC_PROPERTIES_PREFIX)) {
                result.put(entry.getKey(), entry.getValue());
                entries.remove();
            }
        }
        return result;
    }

    private void printVersion() throws IOException {
        var lines = List.of(
                ("MigrateDB version " + BuildInfo.VERSION),
                ("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"),
                (System.getProperty("os.name") + " " + System.getProperty("os.version") + " " +
                 System.getProperty("os.arch") + "\n"));
        if (arguments.shouldOutputJson()) {
            printJson(new ErrorOutput(ErrorCode.CLI_USAGE,
                                      String.join("\n", lines),
                                      null,
                                      null));
        } else {
            lines.forEach(stdout::println);
        }
    }

    private void printUsage() throws IOException {
        var usageLines = List.of(
                ("Usage"),
                ("====="),
                (""),
                ("migratedb [options] command"),
                (""),
                ("By default, the configuration will be read from conf/migratedb.conf."),
                ("Options passed from the command-line override the configuration."),
                (""),
                ("Commands"),
                ("--------"),
                ("migrate  : Migrates the database"),
                ("info     : Prints the information about applied, current and pending migrations"),
                ("validate : Validates the applied migrations against the ones on the classpath"),
                ("baseline : Baselines an existing database at the baselineVersion"),
                ("repair   : Repairs the schema history table"),
                ("repair   : Repairs the schema history table"),
                ("liberate : Converts from Flyway to MigrateDB"),
                (""),
                ("Options (Format: -key=value)"),
                ("-------"),
                ("driver                       : Fully qualified classname of the JDBC driver"),
                ("url                          : JDBC url to use to connect to the database"),
                ("user                         : User to use to connect to the database"),
                ("password                     : Password to use to connect to the database"),
                ("connectRetries               : Maximum number of retries when attempting to connect to the database"),
                ("initSql                      : SQL statements to run to initialize a new database connection"),
                ("schemas                      : Comma-separated list of the schemas managed by MigrateDb"),
                ("table                        : Name of MigrateDB's schema history table"),
                ("oldTable                     : Name of previous schema history table"),
                ("locations                    : Classpath locations to scan recursively for migrations"),
                ("failOnMissingLocations       : Whether to fail if a location specified in the migratedb.locations " +
                 "option doesn't exist"),
                ("resolvers                    : Comma-separated list of custom MigrationResolvers"),
                ("skipDefaultResolvers         : Skips default resolvers (jdbc, sql and Spring-jdbc)"),
                ("sqlMigrationPrefix           : File name prefix for versioned SQL migrations"),
                ("repeatableSqlMigrationPrefix : File name prefix for repeatable SQL migrations"),
                ("sqlMigrationSeparator        : File name separator for SQL migrations"),
                ("sqlMigrationSuffixes         : Comma-separated list of file name suffixes for SQL migrations"),
                ("mixed                        : Allow mixing transactional and non-transactional statements"),
                ("encoding                     : Encoding of SQL migrations"),
                ("placeholderReplacement       : Whether placeholders should be replaced"),
                ("placeholders                 : Placeholders to replace in sql migrations"),
                ("placeholderPrefix            : Prefix of every placeholder"),
                ("placeholderSuffix            : Suffix of every placeholder"),
                ("scriptPlaceholderPrefix      : Prefix of every script placeholder"),
                ("scriptPlaceholderSuffix      : Suffix of every script placeholder"),
                ("lockRetryCount               : The maximum number of retries when trying to obtain a lock"),
                ("jdbcProperties               : Properties to pass to the JDBC driver object"),
                ("installedBy                  : Username that will be recorded in the schema history table"),
                ("target                       : Target version up to which MigrateDB should use migrations"),
                ("outOfOrder                   : Allows migrations to be run \"out of order\""),
                ("callbacks                    : Comma-separated list of MigrateDbCallback classes"),
                ("skipDefaultCallbacks         : Skips default callbacks (sql)"),
                ("validateOnMigrate            : Validate when running migrate"),
                ("validateMigrationNaming      : Validate file names of SQL migrations (including callbacks)"),
                ("ignoreMissingMigrations      : Allow missing migrations when validating"),
                ("ignoreIgnoredMigrations      : Allow ignored migrations when validating"),
                ("ignorePendingMigrations      : Allow pending migrations when validating"),
                ("ignoreFutureMigrations       : Allow future migrations when validating"),
                ("baselineVersion              : Version to tag schema with when executing baseline"),
                ("baselineDescription          : Description to tag schema with when executing baseline"),
                ("baselineOnMigrate            : Baseline on migrate against uninitialized non-empty schema"),
                ("configFiles                  : Comma-separated list of config files to use"),
                ("configFileEncoding           : Encoding to use when loading the config files"),
                ("jarDirs                      : Comma-separated list of dirs for Jdbc drivers & Java migrations"),
                ("createSchemas                : Whether MigrateDB should attempt to create the schemas specified " +
                 "in the schemas property"),
                ("outputFile                   : Send output to the specified file alongside the console"),
                ("outputType                   : Serialise the output in the given format, Values: json"),
                (""),
                ("Flags"),
                ("-----"),
                ("-X              : Print debug output"),
                ("-q              : Suppress all output, except for errors and warnings"),
                ("-n              : Suppress prompting for a user and password"),
                ("--version, -v   : Print the MigrateDB version and exit"),
                ("--help, -h, -?  : Print this usage info and exit"),
                (""),
                ("Example"),
                ("-------"),
                ("migratedb -user=myuser -password=s3cr3t -url=jdbc:h2:mem -placeholders.abc=def migrate")
        );
        if (arguments.shouldOutputJson()) {
            printJson(new ErrorOutput(ErrorCode.CLI_USAGE,
                                      String.join("\n", usageLines),
                                      null,
                                      null));
        } else {
            usageLines.forEach(stdout::println);
        }
    }

    private List<Path> getJdbcDriverJarFiles() throws IOException {
        try (var fileStream = Files.list(driversDir)) {
            return fileStream
                    .filter(it -> it.getFileName().toString().endsWith(".jar"))
                    .collect(Collectors.toList());

        } catch (NotDirectoryException | NoSuchFileException e) {
            LOG.warn("Directory for JDBC Drivers not found: " + driversDir.toAbsolutePath());
            return Collections.emptyList();
        }
    }

    private List<Path> getJavaMigrationJarFiles(Map<String, String> config) throws IOException {
        String jarDirs = config.get(CommandLineConfigKey.JAR_DIRS);
        if (!StringUtils.hasLength(jarDirs)) {
            return Collections.emptyList();
        }

        jarDirs = jarDirs.replace(File.pathSeparator, ",");
        String[] dirs = StringUtils.tokenizeToStringArray(jarDirs, ",");

        List<Path> jarFiles = new ArrayList<>();
        for (String dirName : dirs) {
            try (var fileStream = Files.list(fileSystem.getPath(dirName))) {
                jarFiles.addAll(
                        fileStream.filter(it -> it.getFileName().toString().endsWith(".jar"))
                                  .collect(Collectors.toList())
                );
            } catch (NotDirectoryException | NoSuchFileException e) {
                LOG.warn("Directory for Java migrations not found: " + dirName);
            }
        }
        return jarFiles;
    }

    private void loadConfigurationFromConfigFiles(Map<String, String> config,
                                                  Map<String, String> envVars) {
        var encoding = determineConfigurationFileEncoding(arguments, envVars);

        config.putAll(loadDefaultConfigurationFiles(encoding));

        for (var configFile : determineConfigFilesFromArgs(arguments, envVars)) {
            config.putAll(loadConfigurationFile(configFile, encoding, true));
        }
    }

    /**
     * If no user or password has been provided, prompt for it. If you want to avoid the prompt, pass in an empty user
     * or password.
     *
     * @param config The properties object to load to configuration into.
     */
    private void promptForCredentialsIfMissing(Map<String, String> config, DriverSupport driverSupport) {
        if (console == null) {
            // We are running in an automated build or without tty. Prompting is not possible.
            return;
        }

        if (!config.containsKey(CommandLineConfigKey.URL)) {
            // URL is not set. We are doomed for failure anyway.
            return;
        }

        String url = config.get(CommandLineConfigKey.URL);
        if (!config.containsKey(CommandLineConfigKey.USER)
            && needsUser(url, driverSupport)) {
            config.put(CommandLineConfigKey.USER, console.readLine("Database user: "));
        }

        if (!config.containsKey(CommandLineConfigKey.PASSWORD) &&
            needsPassword(url, driverSupport)) {
            char[] password = console.readPassword("Database password: ");
            config.put(CommandLineConfigKey.PASSWORD, password == null ? "" : String.valueOf(password));
        }
    }

    /**
     * Detect whether the JDBC URL specifies a known authentication mechanism that does not need a username.
     */
    private boolean needsUser(String url, DriverSupport driverSupport) {
        return driverSupport.detectUserRequiredByUrl(url);
    }

    /**
     * Detect whether the JDBC URL specifies a known authentication mechanism that does not need a password.
     */
    private boolean needsPassword(String url, DriverSupport driverSupport) {
        return driverSupport.detectPasswordRequiredByUrl(url);
    }

    private List<Path> determineConfigFilesFromArgs(Arguments arguments,
                                                    Map<String, String> envVars) {
        List<Path> configFiles = new ArrayList<>();

        String workingDirectory = arguments.isWorkingDirectorySet() ? arguments.getWorkingDirectory() : ".";

        if (envVars.containsKey(CommandLineConfigKey.CONFIG_FILES)) {
            for (String file : StringUtils.tokenizeToStringArray(envVars.get(CommandLineConfigKey.CONFIG_FILES), ",")) {
                configFiles.add(fileSystem.getPath(workingDirectory, file));
            }
            return configFiles;
        }

        for (String file : arguments.getConfigFiles()) {
            configFiles.add(fileSystem.getPath(workingDirectory, file));
        }

        return configFiles;
    }

    /**
     * @return The encoding. (default: UTF-8)
     */
    private String determineConfigurationFileEncoding(Arguments arguments,
                                                      Map<String, String> envVars) {
        if (envVars.containsKey(CommandLineConfigKey.CONFIG_FILE_ENCODING)) {
            return envVars.get(CommandLineConfigKey.CONFIG_FILE_ENCODING);
        }

        if (arguments.isConfigFileEncodingSet()) {
            return arguments.getConfigFileEncoding();
        }

        return "UTF-8";
    }

    private static <T> Supplier<T> lazy(Callable<T> factory) {
        return new Supplier<T>() {
            private @MonotonicNonNull T lazyValue;

            @Override
            public T get() {
                if (lazyValue == null) {
                    try {
                        lazyValue = factory.call();
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new MigrateDbException(e);
                    }
                }
                return lazyValue;
            }
        };
    }

    private static DownloadDriversCommand.DriverDefinitions parseDriverDefinitions(Path file) {
        try (var stream = new BufferedInputStream(Files.newInputStream(file))) {
            return new Yaml().loadAs(stream, DownloadDriversCommand.DriverDefinitions.class);
        } catch (IOException e) {
            throw new MigrateDbException("Cannot parse driver definitions from '" + file + "'");
        }
    }
}
