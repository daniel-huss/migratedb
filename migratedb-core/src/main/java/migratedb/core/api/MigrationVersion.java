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
package migratedb.core.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MigrationVersion implements Comparable<MigrationVersion> {
    /**
     * Version for an empty schema. Only a marker.
     */
    public static final MigrationVersion EMPTY = new MigrationVersion(null, "<< Empty Schema >>");
    /**
     * Latest version. Only a marker. For the real version use MigrateDb.info().latest() instead.
     */
    public static final MigrationVersion LATEST = new MigrationVersion(BigInteger.valueOf(-1), "<< Latest Version >>");
    /**
     * Current version. Only a marker. For the real version use MigrateDb.info().current() instead.
     */
    public static final MigrationVersion CURRENT = new MigrationVersion(BigInteger.valueOf(-2),
                                                                        "<< Current Version >>");
    /**
     * Next version. Only a marker. For the real version use MigrateDb.info().next() instead.
     */
    public static final MigrationVersion NEXT = new MigrationVersion(BigInteger.valueOf(-3), "<< Next Version >>");

    /**
     * Regex for matching proper version format
     */
    private static final Pattern SPLIT_REGEX = Pattern.compile("\\.(?=\\d)");
    /**
     * The individual parts this version string is composed of. Ex. 1.2.3.4.0 -> [1, 2, 3, 4, 0]
     */
    private final List<BigInteger> versionParts;
    /**
     * The printable text to represent the version.
     */
    private final String displayText;

    /**
     * Create a {@code MigrationVersion} from a version String.
     *
     * @param version The version String. The value {@code current} will be interpreted as the marker {@link #CURRENT},
     *                {@code next} is mapped to {@link #NEXT}, and {@code latest} corresponds to {@link #LATEST}.
     *
     * @return The MigrationVersion
     */
    @SuppressWarnings("ConstantConditions")
    public static MigrationVersion fromVersion(String version) {
        if ("current".equalsIgnoreCase(version)) {
            return CURRENT;
        }
        if ("next".equalsIgnoreCase(version)) {
            return NEXT;
        }
        if ("latest".equalsIgnoreCase(version) || LATEST.getVersion().equals(version)) {
            return LATEST;
        }
        if (version == null) {
            return EMPTY;
        }
        return new MigrationVersion(version);
    }

    /**
     * Creates a Version using this version string.
     *
     * @param version The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021. <br>{@code null}
     *                means that this version refers to an empty schema.
     */
    private MigrationVersion(String version) {
        String normalizedVersion = version.replace('_', '.');
        this.versionParts = tokenize(normalizedVersion);
        this.displayText = normalizedVersion;
    }

    /**
     * @param version     The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021. <br>{@code
     *                    null} means that this version refers to an empty schema.
     * @param displayText The alternative text to display instead of the version number.
     */
    private MigrationVersion(BigInteger version, String displayText) {
        this.versionParts = new ArrayList<>();
        versionParts.add(version);
        this.displayText = displayText;
    }

    @Override
    public String toString() {
        return displayText;
    }

    /**
     * @return Numeric version as String, {@code null} if this is the EMPTY marker.
     */
    public @Nullable String getVersion() {
        if (equals(EMPTY)) {
            return null;
        }
        if (equals(LATEST)) {
            return Long.toString(Long.MAX_VALUE);
        }
        return displayText;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MigrationVersion)) {
            return false;
        }
        return compareTo((MigrationVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        return versionParts == null ? 0 : versionParts.hashCode();
    }

    /**
     * Convenience method for quickly checking whether this version is at least as new as this other version.
     *
     * @param otherVersion The other version.
     *
     * @return {@code true} if this version is equal or newer, {@code false} if it is older.
     */
    public boolean isAtLeast(String otherVersion) {
        return compareTo(MigrationVersion.fromVersion(otherVersion)) >= 0;
    }

    /**
     * Convenience method for quickly checking whether this version is newer than this other version.
     *
     * @param otherVersion The other version.
     *
     * @return {@code true} if this version is newer, {@code false} if it is not.
     */
    public boolean isNewerThan(String otherVersion) {
        return compareTo(MigrationVersion.fromVersion(otherVersion)) > 0;
    }

    /**
     * Convenience method for quickly checking whether this major version is newer than this other major version.
     *
     * @param otherVersion The other version.
     *
     * @return {@code true} if this major version is newer, {@code false} if it is not.
     */
    public boolean isMajorNewerThan(String otherVersion) {
        return getMajor().compareTo(MigrationVersion.fromVersion(otherVersion).getMajor()) > 0;
    }

    /**
     * @return The major version.
     */
    public BigInteger getMajor() {
        return versionParts.get(0);
    }

    /**
     * @return The major version as a string.
     */
    public String getMajorAsString() {
        return Objects.toString(versionParts.get(0), null);
    }

    /**
     * @return The minor version as a string.
     */
    public String getMinorAsString() {
        if (versionParts.size() == 1) {
            return "0";
        }
        return Objects.toString(versionParts.get(1), null);
    }

    @Override
    public int compareTo(MigrationVersion o) {
        // Ugh! So apparently, EMPTY, CURRENT and NEXT have no well-defined order except "less than others"
        // while LATEST is the greatest value, even greater than versions whose major version is > Long.MAX_VALUE...

        if (this == o) {
            return 0;
        }

        if (o == null) {
            return 1;
        }

        if (this == EMPTY || this == CURRENT || this == NEXT) {
            return -1;
        }

        if (this == LATEST) {
            return 1;
        }

        if (o == EMPTY || o == CURRENT || o == NEXT) {
            return 1;
        }

        if (o == LATEST) {
            return -1;
        }

        List<BigInteger> parts1 = versionParts;
        List<BigInteger> parts2 = o.versionParts;
        int largestNumberOfParts = Math.max(parts1.size(), parts2.size());
        for (int i = 0; i < largestNumberOfParts; i++) {
            int compared = getOrZero(parts1, i).compareTo(getOrZero(parts2, i));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private BigInteger getOrZero(List<BigInteger> elements, int i) {
        return i < elements.size() ? elements.get(i) : BigInteger.ZERO;
    }

    /**
     * Splits this string into list of BigIntegers
     *
     * @param versionStr The string to split.
     *
     * @return The resulting array.
     */
    private List<BigInteger> tokenize(String versionStr) {
        List<BigInteger> parts = new ArrayList<>();
        for (String part : SPLIT_REGEX.split(versionStr)) {
            parts.add(toBigInteger(versionStr, part));
        }

        for (int i = parts.size() - 1; i > 0; i--) {
            if (!parts.get(i).equals(BigInteger.ZERO)) {
                break;
            }
            parts.remove(i);
        }

        return parts;
    }

    private BigInteger toBigInteger(String versionStr, String part) {
        try {
            return new BigInteger(part);
        } catch (NumberFormatException e) {
            // Avoid arbitrary user input in exception message
            var invalidValue = versionStr.length() > 20 ? (versionStr.substring(0, 17) + "...") : versionStr;
            throw new MigrateDbException("Version may only contain 0..9 and . (dot). Invalid version: " + invalidValue);
        }
    }
}
