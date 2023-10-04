/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.internal.configuration;

import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.MigrateDbException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static migratedb.v1.core.internal.sqlscript.SqlScriptMetadataImpl.isMultilineBooleanExpression;

public final class ConfigUtils {
    /**
     * Reads the configuration from a Reader.
     *
     * @return The properties from the configuration file. An empty Map if none.
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
                    } else if (nextLine.trim().startsWith("migratedb.") && nextLine.contains("=")) {
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
     * @param config The config.
     * @param key    The property name.
     * @return The property value as a boolean if it exists, otherwise {@code null}.
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
     * @return The property value as an integer if it exists, otherwise {@code null}.
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
     * @param prefix The expected prefix for MigrateDB configuration parameters. {@code null} if none.
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
                                           String.join(",", unknownMigrateDbProperties));
            throw new MigrateDbException(message, ErrorCode.CONFIGURATION);
        }
    }
}
