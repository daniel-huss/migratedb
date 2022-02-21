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

package migratedb.core.internal.util;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import migratedb.core.api.ClassProvider;
import migratedb.core.api.Location;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.resource.Resource;

/**
 * (Almost) drop-in replacement for class path scanning stuff.
 */
public final class LocationScanner<T> implements ClassProvider<T>, ResourceProvider {
    private final Class<T> supertype;
    private final Collection<ResourceProvider> resourceProviders;
    private final Collection<ClassProvider<?>> classProviders;
    private final ClassLoader classLoader;
    private final boolean failOnMissingLocations;

    private Collection<Class<? extends T>> scannedClasses;

    public LocationScanner(Class<T> supertype,
                           Collection<Location> locations,
                           ClassLoader classLoader,
                           boolean failOnMissingLocations) {
        this.supertype = supertype;
        this.resourceProviders = locations.stream().map(Location::resourceProvider).collect(toUnmodifiableList());
        this.classProviders = locations.stream().map(Location::classProvider).collect(toUnmodifiableList());
        this.classLoader = classLoader;
        if (failOnMissingLocations) {
            locations.stream()
                    .filter(it -> !it.exists())
                    .findFirst()
                    .ifPresent(it -> {
                        throw new MigrateDbException("Location does not exist: " + it);
                    });

        }
        this.failOnMissingLocations = failOnMissingLocations;
    }

    @SuppressWarnings("unchecked")  // checked via isAssignableFrom
    @Override
    public Collection<Class<? extends T>> getClasses() {
        var result = scannedClasses;
        if (result == null) {
            result = classProviders.stream()
                    .flatMap(it -> it.getClasses().stream())
                    .filter(it -> supertype.isAssignableFrom(it) &&
                            !it.isInterface() &&
                            !Modifier.isAbstract(it.getModifiers()) &&
                            Modifier.isPublic(it.getModifiers())
                    )
                    .map(it -> (Class<? extends T>) it)
                    .collect(toUnmodifiableList());
            scannedClasses = result;
        }
        return result;
    }

    @Override
    public Resource getResource(String name) {
        for (var provider : resourceProviders) {
            var resource = provider.getResource(name);
            if (resource != null) return resource;
        }
        return null;
    }

    @Override
    public Collection<Resource> getResources(String prefix, String... suffixes) {
        var result = new ArrayList<Resource>();
        for (var provider : resourceProviders) {
            result.addAll(provider.getResources(prefix, suffixes));
        }
        return result;
    }
}
