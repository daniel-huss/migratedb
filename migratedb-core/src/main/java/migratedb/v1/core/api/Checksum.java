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

package migratedb.v1.core.api;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.util.Collections.emptyIterator;

/**
 * Digest-based checksum that replaces the old Integer checksum.
 */
public final class Checksum {
    private final byte[] bytes;

    private Checksum(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Checksum)) {
            return false;
        }
        var other = (Checksum) obj;
        return Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    /**
     * Returns Base64-encoded value of this checksum for storage in the schema history table. The output of this method
     * can be passed to the {@link #parse(String)} method in order to reconstitute the checksum.
     *
     * @return This checksum as a Base64-String.
     */
    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Parses the string value of a checksum. This method accepts the output of {@link #toString()}.
     *
     * @param string String form of checksum to parse.
     *
     * @return Reconstituted checksum.
     */
    public static Checksum parse(String string) {
        return new Checksum(Base64.getDecoder().decode(string));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final MessageDigest digest = newMessageDigest();
        /**
         * To make sure the digest changes every time a value is added, even if that value is null or an empty byte
         * array/empty stream.
         */
        private byte counter = 0;

        public Builder addBytes(byte @Nullable [] value) {
            return add(value);
        }

        public Builder addBytes(@Nullable InputStream value) {
            return add(value);
        }

        public Builder addString(@Nullable String value) {
            return add(value == null ? null : value.getBytes(StandardCharsets.UTF_8));
        }

        public Builder addLines(@Nullable Reader value) {
            return add(value == null ? emptyIterator() : utf8LinesOf(value));
        }

        public Builder addNumber(@Nullable BigInteger value) {
            return add(value == null ? null : value.toByteArray());
        }

        public Builder addNumber(@Nullable Long value) {
            return addNumber(value == null ? null : BigInteger.valueOf(value));
        }

        /**
         * Returns the final checksum and resets the state of this builder (as if it were freshly constructed).
         */
        public Checksum build() {
            var checksum = new Checksum(digest.digest());
            counter = 0;
            return checksum;
        }

        private Builder add(Iterator<byte[]> bytes) {
            digest.update(counter++);
            while (bytes.hasNext()) {
                digest.update(bytes.next());
            }
            return this;
        }

        private Builder add(@Nullable InputStream bytes) {
            digest.update(counter++);
            if (bytes != null) {
                try (var out = new DigestOutputStream(discardingOutputStream, digest)) {
                    bytes.transferTo(out);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        private Builder add(byte @Nullable [] bytes) {
            return add(bytes == null ? emptyIterator() : List.of(bytes).iterator());
        }

        private static Iterator<byte[]> utf8LinesOf(Reader reader) {
            var buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
            return buffered.lines()
                           .map(it -> it.getBytes(StandardCharsets.UTF_8))
                           .iterator();
        }

        private static MessageDigest newMessageDigest() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ignored) {
                // Cannot happen, since every JVM must support the algorithm
                throw new AssertionError();
            }
        }

        private static final OutputStream discardingOutputStream = new OutputStream() {
            @Override
            public void write(byte[] b) {
            }

            @Override
            public void write(byte[] b, int off, int len) {
            }

            @Override
            public void write(int b) {
            }
        };
    }
}
