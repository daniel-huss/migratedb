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
package migratedb.v1.core.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {
    @SuppressWarnings("StatementWithEmptyBody")
    public static String trimChar(String s, char c) {
        int start, end;
        for (start = 0; start < s.length() && s.charAt(start) == c; start++) {
        }
        for (end = s.length() - 1; end >= 0 && s.charAt(end) == c; end--) {
        }
        if (start >= end) {
            return "";
        }
        return s.substring(start, end + 1);
    }

    /**
     * Trims or pads (with spaces) this string, so it has this exact length.
     *
     * @param str    The string to adjust. {@code null} is treated as an empty string.
     * @param length The exact length to reach.
     * @return The adjusted string.
     */
    public static String trimOrPad(String str, int length) {
        return trimOrPad(str, length, ' ');
    }

    /**
     * Trims or pads this string, so it has this exact length.
     *
     * @param str     The string to adjust. {@code null} is treated as an empty string.
     * @param length  The exact length to reach.
     * @param padChar The padding character.
     * @return The adjusted string.
     */
    public static String trimOrPad(String str, int length, char padChar) {
        StringBuilder result;
        if (str == null) {
            result = new StringBuilder();
        } else {
            result = new StringBuilder(str);
        }

        if (result.length() > length) {
            return result.substring(0, length);
        }

        while (result.length() < length) {
            result.append(padChar);
        }
        return result.toString();
    }

    /**
     * Replaces all occurrences of this originalToken in this string with this replacementToken.
     *
     * @param str              The string to process.
     * @param originalToken    The token to replace.
     * @param replacementToken The replacement.
     * @return The transformed str.
     */
    public static String replaceAll(String str, String originalToken, String replacementToken) {
        return str.replaceAll(Pattern.quote(originalToken), Matcher.quoteReplacement(replacementToken));
    }

    /**
     * Checks whether this string is not {@code null} and not <i>empty</i>.
     *
     * @param str The string to check.
     * @return {@code true} if it has content, {@code false} if it is {@code null} or blank.
     */
    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Checks whether this string isn't {@code null} and contains at least one non-blank character.
     *
     * @param s The string to check.
     * @return {@code true} if it has text, {@code false} if not.
     */
    public static boolean hasText(String s) {
        return (s != null) && (!s.trim().isEmpty());
    }

    /**
     * Splits this string into an array using these delimiters.
     *
     * @param str        The string to split.
     * @param delimiters The delimiters to use.
     * @return The resulting array.
     */
    public static String[] tokenizeToStringArray(String str, String delimiters) {
        if (str == null) {
            return null;
        }
        Collection<String> tokens = tokenizeToStringCollection(str, delimiters);
        return tokens.toArray(new String[0]);
    }

    /**
     * Splits this string into a collection using these delimiters.
     *
     * @param str        The string to split.
     * @param delimiters The delimiters to use.
     * @return The resulting array.
     */
    public static List<String> tokenizeToStringCollection(String str, String delimiters) {
        if (str == null) {
            return null;
        }
        List<String> tokens = new ArrayList<>(str.length() / 5);
        char[] delimiterChars = delimiters.toCharArray();
        int start = 0;
        int end = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean delimiter = false;
            for (char d : delimiterChars) {
                if (c == d) {
                    tokens.add(str.substring(start, end));
                    start = i + 1;
                    end = start;
                    delimiter = true;
                    break;
                }
            }
            if (!delimiter) {
                if (i == start && c == ' ') {
                    start++;
                    end++;
                }
                if (i >= start && c != ' ') {
                    end = i + 1;
                }
            }
        }
        if (start < end) {
            tokens.add(str.substring(start, end));
        }
        return tokens;
    }

    /**
     * Checks whether {@code str} both begins with this prefix and ends withs either of these suffixes.
     *
     * @param str      The string to check.
     * @param prefix   The prefix.
     * @param suffixes The suffixes.
     * @return {@code true} if it does, {@code false} if not.
     */
    public static boolean startsAndEndsWith(String str, String prefix, Collection<String> suffixes) {
        if (StringUtils.hasLength(prefix) && !str.startsWith(prefix)) {
            return false;
        }
        for (String suffix : suffixes) {
            if (str.endsWith(suffix) && (str.length() > (prefix + suffix).length())) {
                return true;
            }
        }
        return false;
    }
}
