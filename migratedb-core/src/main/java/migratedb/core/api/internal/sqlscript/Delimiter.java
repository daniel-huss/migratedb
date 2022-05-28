/*
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
package migratedb.core.api.internal.sqlscript;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a sql statement delimiter.
 */
public class Delimiter {
    public static final Delimiter SEMICOLON = new Delimiter(";", false

    );
    public static final Delimiter GO = new Delimiter("GO", true

    );

    /**
     * The actual delimiter string.
     */
    private final String delimiter;

    /**
     * Whether the delimiter sits alone on a new line or not.
     */
    private final boolean aloneOnLine;

    /**
     * Creates a new delimiter.
     *
     * @param delimiter   The actual delimiter string.
     * @param aloneOnLine Whether the delimiter sits alone on a new line or not.
     */
    public Delimiter(String delimiter, boolean aloneOnLine

    ) {
        this.delimiter = delimiter;
        this.aloneOnLine = aloneOnLine;

    }

    /**
     * @return The actual delimiter string.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @return Whether the delimiter sits alone on a new line or not.
     */
    public boolean shouldBeAloneOnLine() {
        return aloneOnLine;
    }

    /**
     * @return The escape string for the delimiter, if any.
     */
    public @Nullable String getEscape() {

        return null;
    }

    @Override
    public String toString() {
        return (aloneOnLine ? "\n" : "") + delimiter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Delimiter delimiter1 = (Delimiter) o;

        return aloneOnLine == delimiter1.aloneOnLine && delimiter.equals(delimiter1.delimiter);
    }

    @Override
    public int hashCode() {
        int result = delimiter.hashCode();
        result = 31 * result + (aloneOnLine ? 1 : 0);
        return result;
    }
}
