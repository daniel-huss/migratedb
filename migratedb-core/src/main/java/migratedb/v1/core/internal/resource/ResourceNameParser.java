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
package migratedb.v1.core.internal.resource;

import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.resource.ResourceName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ResourceNameParser {
    private static final class Prefix {
        final String prefix;
        final ResourceType resourceType;

        private Prefix(String prefix, ResourceType resourceType) {
            this.prefix = prefix;
            this.resourceType = resourceType;
        }
    }

    private static final class PrefixAndSuffix {
        final String prefix;
        final String suffix;

        private PrefixAndSuffix(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    private final Configuration configuration;
    private final List<Prefix> prefixes;

    public ResourceNameParser(Configuration configuration) {
        this.configuration = configuration;
        // Versioned and Undo migrations are named in the form prefixVERSIONseparatorDESCRIPTIONsuffix
        // Repeatable migrations and callbacks are named in the form prefixSeparatorDESCRIPTIONsuffix
        prefixes = populatePrefixes(configuration);
    }

    public ResourceName parse(String resourceName) {
        return parse(resourceName, configuration.getSqlMigrationSuffixes());
    }

    public ResourceName parse(String resourceName, Collection<String> suffixes) {
        // Strip off suffixes
        var suffixResult = stripSuffix(resourceName, suffixes);

        // Find the appropriate prefix
        var prefix = findPrefix(suffixResult.prefix, prefixes);
        if (prefix != null) {
            // Strip off prefix
            var prefixResult = stripPrefix(suffixResult.prefix, prefix.prefix);
            assert prefixResult != null;
            String name = prefixResult.suffix;
            var splitName = splitAtSeparator(name, configuration.getSqlMigrationSeparator());
            boolean isValid = true;
            String validationMessage = "";
            String exampleDescription = ("".equals(splitName.suffix)) ? "description" : splitName.suffix;

            // Validate the name
            if (!ResourceType.isVersioned(prefix.resourceType)) {
                // Must not have a version (that is, something before the separator)
                if (!"".equals(splitName.prefix)) {
                    isValid = false;
                    validationMessage = "Invalid repeatable migration / callback name format: " + resourceName
                            + " (It cannot contain a version and should look like this: "
                            + prefixResult.prefix + configuration.getSqlMigrationSeparator() +
                            exampleDescription + suffixResult.suffix + ")";
                }
            } else {
                // Must have a version (that is, something before the separator)
                if ("".equals(splitName.prefix)) {
                    isValid = false;
                    validationMessage = "Invalid versioned migration name format: " + resourceName
                            + " (It must contain a version and should look like this: "
                            + prefixResult.prefix + "1.2" + configuration.getSqlMigrationSeparator() +
                            exampleDescription + suffixResult.suffix + ")";
                } else {
                    // ... and that must be a legitimate version
                    try {
                        Version.parse(splitName.prefix);
                    } catch (RuntimeException e) {
                        isValid = false;
                        validationMessage = "Invalid versioned migration name format: " + resourceName
                                + " (could not recognise version number " + splitName.prefix + ")";
                    }
                }
            }

            String description = splitName.suffix.replace("_", " ");
            return new ResourceName(prefixResult.prefix,
                    splitName.prefix,
                    configuration.getSqlMigrationSeparator(),
                    description,
                    splitName.suffix,
                    suffixResult.suffix,
                    isValid,
                    validationMessage);
        }

        // Didn't match any prefix
        return ResourceName.invalid("Unrecognised migration name format: " + resourceName);
    }

    private Prefix findPrefix(String nameWithoutSuffix, List<Prefix> prefixes) {
        for (var prefix : prefixes) {
            if (nameWithoutSuffix.startsWith(prefix.prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private PrefixAndSuffix stripSuffix(String name, Collection<String> suffixes) {
        for (var suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return new PrefixAndSuffix(name.substring(0, name.length() - suffix.length()), suffix);
            }
        }
        return new PrefixAndSuffix(name, "");
    }

    private PrefixAndSuffix stripPrefix(String fileName, String prefix) {
        if (fileName.startsWith(prefix)) {
            return new PrefixAndSuffix(prefix, fileName.substring(prefix.length()));
        }
        return null;
    }

    private PrefixAndSuffix splitAtSeparator(String name, String separator) {
        int separatorIndex = name.indexOf(separator);
        if (separatorIndex >= 0) {
            return new PrefixAndSuffix(name.substring(0, separatorIndex),
                    name.substring(separatorIndex + separator.length()));
        } else {
            return new PrefixAndSuffix(name, "");
        }
    }

    private List<Prefix> populatePrefixes(Configuration configuration) {
        List<Prefix> prefixes = new ArrayList<>();

        prefixes.add(new Prefix(configuration.getSqlMigrationPrefix(), ResourceType.MIGRATION));

        prefixes.add(new Prefix(configuration.getRepeatableSqlMigrationPrefix(), ResourceType.REPEATABLE_MIGRATION));
        for (Event event : Event.values()) {
            prefixes.add(new Prefix(event.getId(), ResourceType.CALLBACK));
        }

        Comparator<Prefix> prefixComparator = (p1, p2) -> {
            // Sort most-hard-to-match first; that is, in descending order of prefix length
            return p2.prefix.length() - p1.prefix.length();
        };

        prefixes.sort(prefixComparator);
        return prefixes;
    }
}
