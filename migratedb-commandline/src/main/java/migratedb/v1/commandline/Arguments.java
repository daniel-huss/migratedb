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

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.MigrationState;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public class Arguments {
    // Flags
    private static final String DEBUG_FLAG = "-X";
    private static final String QUIET_FLAG = "-q";
    private static final String SUPPRESS_PROMPT_FLAG = "-n";
    private static final List<String> PRINT_VERSION_AND_EXIT_FLAGS = Arrays.asList("-v", "--version");
    private static final List<String> PRINT_USAGE_FLAGS = Arrays.asList("-?", "-h", "--help");

    // Command line specific configuration options
    private static final String FILE_SYSTEM_URI = "fileSystemUri";
    private static final String DRIVER_NAMES = "driverNames";
    private static final String INSTALLATION_DIR = "baseDirectory";
    private static final String OUTPUT_FILE = "outputFile";
    private static final String OUTPUT_TYPE = "outputType";
    private static final String CONFIG_FILE_ENCODING = "configFileEncoding";
    private static final String CONFIG_FILES = "configFiles";
    private static final String WORKING_DIRECTORY = "workingDirectory";
    private static final String INFO_SINCE_DATE = "infoSinceDate";
    private static final String INFO_UNTIL_DATE = "infoUntilDate";
    private static final String INFO_SINCE_VERSION = "infoSinceVersion";
    private static final String INFO_UNTIL_VERSION = "infoUntilVersion";
    private static final String INFO_OF_STATE = "infoOfState";

    private static final DateTimeFormatter OLD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm",
            Locale.ROOT);

    private static final List<String> VALID_OPERATIONS_AND_FLAGS = getValidOperationsAndFlags();

    private static List<String> getValidOperationsAndFlags() {
        List<String> operationsAndFlags = new ArrayList<>(Arrays.asList(
                DEBUG_FLAG,
                QUIET_FLAG,
                SUPPRESS_PROMPT_FLAG,
                "help",
                "migrate",
                "info",
                "validate",
                "baseline",
                "repair",
                "download-drivers",
                "liberate"
        ));
        operationsAndFlags.addAll(PRINT_VERSION_AND_EXIT_FLAGS);
        operationsAndFlags.addAll(PRINT_USAGE_FLAGS);
        return operationsAndFlags;
    }

    private final String[] args;

    public Arguments(String[] args) {
        this.args = args;
    }

    private static boolean isFlagSet(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFlagSet(String[] args, List<String> flags) {
        for (String flag : flags) {
            if (isFlagSet(args, flag)) {
                return true;
            }
        }
        return false;
    }

    private static String getArgumentValue(String argName, String[] allArgs) {
        for (String arg : allArgs) {
            if (arg.startsWith("-" + argName + "=")) {
                return parseConfigurationOptionValueFromArg(arg);
            }
        }
        return "";
    }

    private static String parseConfigurationOptionValueFromArg(String arg) {
        int index = arg.indexOf("=");

        if (index < 0 || index == arg.length() - 1) {
            return "";
        }

        return arg.substring(index + 1);
    }

    private static List<String> getOperationsFromArgs(String[] args) {
        List<String> operations = new ArrayList<>();

        for (String arg : args) {
            if (!arg.startsWith("-")) {
                operations.add(arg);
            }
        }
        return operations;
    }

    private static List<String> getConfigFilesFromArgs(String[] args) {
        String configFilesCommaSeparatedList = getArgumentValue(CONFIG_FILES, args);

        return Arrays.asList(StringUtils.tokenizeToStringArray(configFilesCommaSeparatedList, ","));
    }

    private static Map<String, String> getConfigurationFromArgs(String[] args) {
        Map<String, String> configuration = new HashMap<>();

        for (String arg : args) {
            if (isConfigurationArg(arg)) {
                String configurationOptionName = getConfigurationOptionNameFromArg(arg);

                if (!isConfigurationOptionCommandlineOnly(configurationOptionName)) {
                    configuration.put("migratedb." + configurationOptionName,
                            parseConfigurationOptionValueFromArg(arg));
                }
            }
        }

        return configuration;
    }

    private static boolean isConfigurationOptionCommandlineOnly(String configurationOptionName) {
        return OUTPUT_FILE.equals(configurationOptionName) ||
                OUTPUT_TYPE.equals(configurationOptionName) ||
                WORKING_DIRECTORY.equals(configurationOptionName) ||
                INFO_SINCE_DATE.equals(configurationOptionName) ||
                INFO_UNTIL_DATE.equals(configurationOptionName) ||
                INFO_SINCE_VERSION.equals(configurationOptionName) ||
                INFO_UNTIL_VERSION.equals(configurationOptionName) ||
                INFO_OF_STATE.equals(configurationOptionName) ||
                INSTALLATION_DIR.equals(configurationOptionName) ||
                DRIVER_NAMES.equals(configurationOptionName) ||
                FILE_SYSTEM_URI.equalsIgnoreCase(configurationOptionName);
    }

    private static String getConfigurationOptionNameFromArg(String arg) {
        int index = arg.indexOf("=");

        return arg.substring(1, index);
    }

    private static boolean isConfigurationArg(String arg) {
        return arg.startsWith("-") && arg.contains("=");
    }

    public void validate() {
        for (String arg : args) {
            if (!isConfigurationArg(arg) && !VALID_OPERATIONS_AND_FLAGS.contains(arg)) {
                throw new MigrateDbException("Invalid argument: " + arg);
            }
        }

        String outputTypeValue = getArgumentValue(OUTPUT_TYPE, args).toLowerCase(Locale.ENGLISH);
        if (!("json".equals(outputTypeValue) || outputTypeValue.isEmpty())) {
            throw new MigrateDbException(
                    "'" + outputTypeValue + "' is an invalid value for the -outputType option. Use 'json'.");
        }
    }

    public boolean shouldSuppressPrompt() {
        return isFlagSet(args, SUPPRESS_PROMPT_FLAG);
    }

    public boolean shouldPrintVersionAndExit() {
        return isFlagSet(args, PRINT_VERSION_AND_EXIT_FLAGS);
    }

    public boolean shouldOutputJson() {
        return "json".equalsIgnoreCase(getArgumentValue(OUTPUT_TYPE, args));
    }

    public boolean shouldPrintUsage() {
        return (isFlagSet(args, PRINT_USAGE_FLAGS) || getOperations().isEmpty());
    }

    public LogLevel getLogLevel() {
        if (isFlagSet(args, QUIET_FLAG)) {
            return LogLevel.WARN;
        }

        if (isFlagSet(args, DEBUG_FLAG)) {
            return LogLevel.DEBUG;
        }

        return LogLevel.INFO;
    }

    public boolean hasOperation(String operation) {
        return getOperations().contains(operation);
    }

    public List<String> getOperations() {
        return getOperationsFromArgs(args);
    }

    public List<String> getConfigFiles() {
        return getConfigFilesFromArgs(args);
    }

    public String getOutputFile() {
        return getArgumentValue(OUTPUT_FILE, args);
    }

    public String getWorkingDirectory() {
        return getArgumentValue(WORKING_DIRECTORY, args);
    }

    public @Nullable Instant getInfoSinceDate() {
        return parseDate(INFO_SINCE_DATE);
    }

    public @Nullable Instant getInfoUntilDate() {
        return parseDate(INFO_UNTIL_DATE);
    }

    public @Nullable Version getInfoSinceVersion() {
        return parseVersion(INFO_SINCE_VERSION);
    }

    public @Nullable Version getInfoUntilVersion() {
        return parseVersion(INFO_UNTIL_VERSION);
    }

    public @Nullable MigrationState getInfoOfState() {
        String stateStr = getArgumentValue(INFO_OF_STATE, args);

        if (!StringUtils.hasText(stateStr)) {
            return null;
        }

        return MigrationState.valueOf(stateStr.toUpperCase(Locale.ENGLISH));
    }

    public String getInstallationDirectory() {
        var arg = getArgumentValue(INSTALLATION_DIR, args);
        return arg.isEmpty() ? System.getProperty("user.dir") : arg;
    }

    public List<String> getDriverNames() {
        String commaSeparatedList = getArgumentValue(DRIVER_NAMES, args);
        return Arrays.asList(StringUtils.tokenizeToStringArray(commaSeparatedList, ","));
    }

    private @Nullable Version parseVersion(String argument) {
        String versionStr = getArgumentValue(argument, args);

        if (versionStr.isEmpty()) {
            return null;
        }

        return Version.parse(versionStr);
    }

    private @Nullable Instant parseDate(String argument) {
        String dateStr = getArgumentValue(argument, args);

        if (dateStr.isEmpty()) {
            return null;
        }

        Instant result = null;
        for (var format : List.of(ISO_DATE_TIME, OLD_DATE_FORMAT)) {
            try {
                var parsed = format.parseBest(dateStr,
                        ZonedDateTime::from,
                        OffsetDateTime::from,
                        LocalDateTime::from);
                if (parsed instanceof ZonedDateTime) {
                    result = ((ZonedDateTime) parsed).toInstant();
                } else if (parsed instanceof OffsetDateTime) {
                    result = ((OffsetDateTime) parsed).toInstant();
                } else if (parsed instanceof LocalDateTime) {
                    result = ((LocalDateTime) parsed).atZone(ZoneId.systemDefault()).toInstant();
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        if (result == null) {
            throw new MigrateDbException("'" + dateStr + "' is an invalid value for the " + argument + " option. " +
                    "The expected format is either ISO-8601 date/time or '" +
                    OLD_DATE_FORMAT + "', like '13/10/2020 16:30'.");
        }
        return result;
    }

    public boolean isOutputFileSet() {
        return !getOutputFile().isEmpty();
    }

    public boolean isWorkingDirectorySet() {
        return !getWorkingDirectory().isEmpty();
    }

    public String getConfigFileEncoding() {
        return getArgumentValue(CONFIG_FILE_ENCODING, args);
    }

    public boolean isConfigFileEncodingSet() {
        return !getConfigFileEncoding().isEmpty();
    }

    public Map<String, String> getConfiguration() {
        return getConfigurationFromArgs(args);
    }

    public FileSystem getFileSystem() {
        var uri = getArgumentValue(FILE_SYSTEM_URI, args);
        if (uri.isEmpty()) {
            return FileSystems.getDefault();
        } else {
            return FileSystems.getFileSystem(URI.create(uri));
        }
    }
}
