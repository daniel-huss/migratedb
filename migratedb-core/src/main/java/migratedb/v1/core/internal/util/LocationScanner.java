/*
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

package migratedb.v1.core.internal.util;

import migratedb.v1.core.api.ClassProvider;
import migratedb.v1.core.api.Location;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.ResourceProvider;
import migratedb.v1.core.api.resource.Resource;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * (Almost) drop-in replacement for class path scanning stuff.
 */
public final class LocationScanner<T> implements ClassProvider<T>, ResourceProvider {
    private final Class<T> supertype;
    private final Collection<ResourceProvider> resourceProviders;
    private final Collection<ClassProvider<?>> classProviders;

    private Collection<Class<? extends T>> scannedClasses;

    public LocationScanner(Class<T> supertype,
                           Collection<Location> locations,
                           ClassLoader classLoader,
                           boolean failOnMissingLocations) {
        this.supertype = supertype;
        this.resourceProviders = locations.stream().map(Location::resourceProvider).collect(toUnmodifiableList());
        this.classProviders = locations.stream().map(Location::classProvider).collect(toUnmodifiableList());
        if (failOnMissingLocations) {
            locations.stream()
                     .filter(it -> !it.exists())
                     .findFirst()
                     .ifPresent(it -> {
                         throw new MigrateDbException("Location does not exist: " + it);
                     });

        }
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
    public @Nullable Resource getResource(String name) {
        for (var provider : resourceProviders) {
            var resource = provider.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Collection<Resource> getResources(String prefix, Collection<String> suffixes) {
        var result = new ArrayList<Resource>();
        for (var provider : resourceProviders) {
            result.addAll(provider.getResources(prefix, suffixes));
        }
        return result;
    }
}
