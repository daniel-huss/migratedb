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
package migratedb.core.internal.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import migratedb.core.api.Location;
import migratedb.core.api.Location.FileSystemLocation;
import migratedb.core.api.logging.Log;

/**
 * Encapsulation of a location list. Removes duplicates and sub-locations (for file-based locations).
 */
public class Locations {
    private static final Log LOG = Log.getLog(Locations.class);

    private final List<Location> locations;

    /**
     * Creates a new Locations wrapper with these locations.
     *
     * @param rawLocations The locations to process.
     */
    public Locations(List<Location> rawLocations) {
        this.locations = processLocations(rawLocations);
    }

    /**
     * Creates a new Locations wrapper with these locations.
     *
     * @param rawLocations The locations to process.
     */
    public Locations(List<String> rawLocations, ClassLoader classLoader) {
        this.locations = processLocations(rawLocations.stream()
                .map(it -> Location.parse(it, classLoader))
                .collect(Collectors.toUnmodifiableList()));
    }

    private static List<Location> processLocations(List<Location> locations) {
        var processed = new LinkedHashSet<Location>(locations.size());
        for (Location location : locations) {
            if (processed.contains(location)) {
                LOG.warn("Discarding duplicate location '" + location + "'");
                continue;
            }

            if (location instanceof FileSystemLocation) {
                Location parentLocation = getParentLocationIfExists((FileSystemLocation) location, processed);
                if (parentLocation != null) {
                    LOG.warn("Discarding location '" + location + "' as it is a sublocation of '" + parentLocation + "'");
                    continue;
                }
            }
            processed.add(location);
        }
        return processed.stream().collect(Collectors.toUnmodifiableList());
    }

    /**
     * @return The locations.
     */
    public List<Location> getLocations() {
        return locations;
    }

    /**
     * Retrieves this location's parent within this list, if any.
     *
     * @param location       The location to check.
     * @param otherLocations The list to search.
     * @return The parent location. {@code null} if none.
     */
    private static Location getParentLocationIfExists(FileSystemLocation location, Collection<Location> otherLocations) {
        for (var otherLocation : otherLocations) {
            if (otherLocation instanceof FileSystemLocation &&
                    isParent(((FileSystemLocation) otherLocation), location)) {
                return otherLocation;
            }
        }
        return null;
    }

    private static boolean isParent(FileSystemLocation maybeParent, FileSystemLocation maybeChild) {
        var parentDir = maybeParent.getBaseDirectory();
        var childDir = maybeChild.getBaseDirectory();
        if (parentDir.equals(childDir) || childDir.getNameCount() < parentDir.getNameCount()) return false;
        do {
            childDir = childDir.getParent();
            if (parentDir.equals(childDir)) return true;
        } while (childDir != null && !childDir.equals(childDir.getParent()));
        return false;
    }
}
