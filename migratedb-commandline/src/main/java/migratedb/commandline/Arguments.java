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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.MigrationState;
import migratedb.core.api.MigrationVersion;
import migratedb.core.internal.util.MigrateDbWebsiteLinks;
import migratedb.core.internal.util.StringUtils;

public class Arguments {
    // Flags
    private static final String DEBUG_FLAG = "-X";
    private static final String QUIET_FLAG = "-q";
    private static final String SUPPRESS_PROMPT_FLAG = "-n";
    private static final List<String> PRINT_VERSION_AND_EXIT_FLAGS = Arrays.asList("-v", "--version");
    private static final List<String> PRINT_USAGE_FLAGS = Arrays.asList("-?", "-h", "--help");

    // Command line specific configuration options
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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static final List<String> VALID_OPERATIONS_AND_FLAGS = getValidOperationsAndFlags();

    private static List<String> getValidOperationsAndFlags() {
        List<String> operationsAndFlags = new ArrayList<>(Arrays.asList(
            DEBUG_FLAG,
            QUIET_FLAG,
            SUPPRESS_PROMPT_FLAG,
            "help",
            "migrate",
            "clean",
            "info",
            "validate",
            "undo",
            "baseline",
            "repair"
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

        if (index < 0 || index == arg.length()) {
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
               INFO_OF_STATE.equals(configurationOptionName);
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
        if (!("json".equals(outputTypeValue) || "".equals(outputTypeValue))) {
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

    public Date getInfoSinceDate() {
        return parseDate(INFO_SINCE_DATE);
    }

    public Date getInfoUntilDate() {
        return parseDate(INFO_UNTIL_DATE);
    }

    public MigrationVersion getInfoSinceVersion() {
        return parseVersion(INFO_SINCE_VERSION);
    }

    public MigrationVersion getInfoUntilVersion() {
        return parseVersion(INFO_UNTIL_VERSION);
    }

    public MigrationState getInfoOfState() {
        String stateStr = getArgumentValue(INFO_OF_STATE, args);

        if (!StringUtils.hasText(stateStr)) {
            return null;
        }

        return MigrationState.valueOf(stateStr.toUpperCase(Locale.ENGLISH));
    }

    private MigrationVersion parseVersion(String argument) {
        String versionStr = getArgumentValue(argument, args);

        if (versionStr.isEmpty()) {
            return null;
        }

        return MigrationVersion.fromVersion(versionStr);
    }

    private Date parseDate(String argument) {
        String dateStr = getArgumentValue(argument, args);

        if (dateStr.isEmpty()) {
            return null;
        }

        try {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.parse(dateStr);
            }
        } catch (ParseException e) {
            throw new MigrateDbException("'" + dateStr + "' is an invalid value for the " + argument + " option. " +
                                         "The expected format is 'dd/mm/yyyy hh:mm', like '13/10/2020 16:30'. " +
                                         "See the MigrateDb documentation for help: " +
                                         MigrateDbWebsiteLinks.FILTER_INFO_OUTPUT);
        }
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
}
