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
package migratedb.v1.core.internal.resolver;

import migratedb.v1.core.api.Checksum;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public enum ChecksumCalculator {
    ;

    /**
     * Calculates the checksum of these resources. The checksum is line-ending independent.
     *
     * @return A checksum for the given resources.
     */
    public static Checksum calculate(Collection<Resource> resources, Configuration configuration) {
        var sortedResources = new ArrayList<>(resources);
        sortedResources.sort(Comparator.comparing(Resource::getName));
        var builder = Checksum.builder();
        for (var resource : sortedResources) {
            try (var reader = resource.read(configuration.getEncoding())) {
                builder.addLines(reader);
            } catch (IOException e) {
                throw new MigrateDbException(
                    "Unable to calculate checksum of " + resource.getName() + "\n" + e.getMessage(), e);
            }
        }
        return builder.build();
    }
}
