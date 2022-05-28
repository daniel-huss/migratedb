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

import migratedb.core.api.resource.Resource;

import java.util.Collection;
import java.util.Collections;

/**
 * A facility to obtain loadable resources.
 */
public interface ResourceProvider {
    /**
     * Retrieves the resource with this name.
     *
     * @param name The name of the resource.
     * @return The resource or {@code null} if not found.
     */
    Resource getResource(String name);

    /**
     * Retrieve all resources whose last name component begins with this prefix and ends with any of these suffixes.
     *
     * @param prefix   The prefix.
     * @param suffixes The suffixes.
     * @return The matching resources.
     */
    Collection<Resource> getResources(String prefix, String... suffixes);


    /**
     * @return Instance that never provides any resoures.
     */
    static ResourceProvider noResources() {
        return new ResourceProvider() {

            @Override
            public Resource getResource(String name) {
                return null;
            }

            @Override
            public Collection<Resource> getResources(String prefix, String... suffixes) {
                return Collections.emptyList();
            }
        };
    }
}
