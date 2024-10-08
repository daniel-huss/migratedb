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
package migratedb.v1.core.api.internal.resource;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.Version;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a resource name, parsed into its components.
 * <p>
 * Versioned and Undo migrations are named in the form prefixVERSIONseparatorDESCRIPTIONsuffix; Repeatable migrations
 * and callbacks are named in the form prefixSeparatorDESCRIPTIONsuffix
 */
public final class ResourceName {
    private final String prefix;
    private final String version;
    private final String separator;
    private final String description;
    private final String rawDescription;
    private final String suffix;
    private final boolean isValid;
    private final String validityMessage;

    public ResourceName(String prefix, String version, String separator, String description, String rawDescription,
                        String suffix, boolean isValid, String validityMessage) {
        this.prefix = prefix;
        this.version = version;
        this.separator = separator;
        this.description = description;
        this.rawDescription = rawDescription;
        this.suffix = suffix;
        this.isValid = isValid;
        this.validityMessage = validityMessage;
    }

    /**
     * Construct a result representing an invalid resource name
     *
     * @param message A message explaining the reason the resource name is invalid
     *
     * @return The fully populated parsing result.
     */
    public static ResourceName invalid(String message) {
        return new ResourceName(null, null, null, null, null,
                                null, false, message);
    }

    /**
     * The prefix of the resource (eg. "V" for versioned migrations)
     */
    public String getPrefix() {
        if (!isValid) {
            throw new MigrateDbException(
                "Cannot access prefix of invalid ResourceNameParseResult\n" + validityMessage);
        }
        return prefix;
    }

    private boolean isVersioned() {
        return (!"".equals(version));
    }

    /**
     * The version of the resource (eg. "1.2.3" for versioned migrations), or null for non-versioned resources
     */
    public @Nullable Version getVersion() {
        if (isVersioned()) {
            return Version.parse(version);
        } else {
            return null;
        }
    }

    /**
     * The description of the resource
     */
    public String getDescription() {
        if (!isValid) {
            throw new MigrateDbException(
                "Cannot access description of invalid ResourceName\n" + validityMessage);
        }
        return description;
    }

    /**
     * The file type suffix of the resource (eg. ".sql" for SQL migration scripts)
     */
    public String getSuffix() {
        if (!isValid) {
            throw new MigrateDbException(
                "Cannot access suffix of invalid ResourceName\n" + validityMessage);
        }
        return suffix;
    }

    /**
     * The full name of the resource
     */
    public String getFilenameWithoutSuffix() {
        if (!isValid) {
            throw new MigrateDbException("Cannot access name of invalid ResourceName\n" + validityMessage);
        }

        if ("".equals(description)) {
            return prefix + version;
        } else {
            return prefix + version + separator + description;
        }
    }

    /**
     * The filename of the resource as it appears on disk
     */
    public String getFilename() {
        if (!isValid) {
            throw new MigrateDbException("Cannot access name of invalid ResourceName\n" + validityMessage);
        }

        if ("".equals(description)) {
            return prefix + version + suffix;
        } else {
            return prefix + version + separator + rawDescription + suffix;
        }
    }

    /**
     * Whether the resource name was successfully parsed.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * If the resource name was not successfully parsed, an explanation of the problem.
     */
    public String getValidityMessage() {
        return validityMessage;
    }
}
