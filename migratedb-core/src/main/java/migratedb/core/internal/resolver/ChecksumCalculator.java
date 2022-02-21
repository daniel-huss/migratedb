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
package migratedb.core.internal.resolver;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.resource.Resource;

public enum ChecksumCalculator {
    ;

    /**
     * Calculates the checksum of these resources. The checksum is line-ending independent.
     *
     * @return A checksum for the given resources.
     */
    public static int calculate(List<Resource> resources) {
        var digest = newMessageDigest();
        for (var resource : resources) {
            calculateChecksumForResource(resource, digest);
        }
        return ByteBuffer.wrap(digest.digest()).getInt(); // what a shame to only use the first 4 bytes...
    }

    private static MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
            // Cannot happen, since every JVM supports MD5
            throw new AssertionError();
        }
    }

    private static void calculateChecksumForResource(Resource resource, MessageDigest digest) {
        // Only ISO_8859_1 provides a mapping for each byte
        try (var reader = new BufferedReader(resource.read(ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                digest.update(line.getBytes(ISO_8859_1));
            }
        } catch (IOException e) {
            throw new MigrateDbException("Unable to calculate checksum of " + resource.getName() + "\n" + e.getMessage(), e);
        }
    }
}
