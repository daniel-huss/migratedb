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

import migratedb.core.internal.resource.classpath.ClassPathResourceProvider;
import migratedb.core.internal.resource.filesystem.FileSystemResourceProvider;
import migratedb.core.internal.util.ClassUtils;
import migratedb.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A location to load migrations from.
 * <p>
 * Note: Although this class is declared abstract, it is not meant to be subclassed outside its compilation unit. To
 * provide a custom location type, use {@link CustomLocation}.
 */
public abstract class Location {

    // Prevent subclassing outside this compilation unit
    private Location() {
    }

    public static Location parse(String locationString, @Nullable ClassLoader classLoader) {
        if (locationString.startsWith(FileSystemLocation.PREFIX)) {
            var baseDirectory = Paths.get(locationString.substring(FileSystemLocation.PREFIX.length()));
            return new FileSystemLocation(baseDirectory);
        } else if (locationString.startsWith(CustomLocation.PREFIX)) {
            var providerClass = locationString.substring(CustomLocation.PREFIX.length());
            return CustomLocation.fromClass(providerClass, classLoader);
        } else {
            String packageName;
            if (locationString.startsWith(ClassPathLocation.PREFIX)) {
                packageName = locationString.substring(ClassPathLocation.PREFIX.length());
            } else {
                packageName = locationString;
            }
            return new ClassPathLocation(packageName, classLoader);
        }
    }

    public abstract ResourceProvider resourceProvider();

    public abstract ClassProvider<?> classProvider();

    public abstract boolean exists();

    public static final class CustomLocation extends Location {
        /**
         * The prefix for custom location implementations.
         */
        public static final String PREFIX = "custom:";

        private final ClassProvider<?> classProvider;
        private final ResourceProvider resourceProvider;

        public static CustomLocation fromClass(String className, @Nullable ClassLoader classLoader) {
            var providerInstance = ClassUtils.instantiate(className, classLoader);
            var errorPrefix = "Location '" + CustomLocation.PREFIX + className + "' must implement ";
            if (!(providerInstance instanceof ClassProvider)) {
                throw new MigrateDbException(errorPrefix + ClassProvider.class.getName());
            }
            if (!(providerInstance instanceof ResourceProvider)) {
                throw new MigrateDbException(errorPrefix + ResourceProvider.class.getName());
            }
            return new CustomLocation((ClassProvider<?>) providerInstance, (ResourceProvider) providerInstance);
        }

        public CustomLocation(ClassProvider<?> classProvider, ResourceProvider resourceProvider) {
            this.classProvider = classProvider;
            this.resourceProvider = resourceProvider;
        }

        @Override
        public ResourceProvider resourceProvider() {
            return resourceProvider;
        }

        @Override
        public ClassProvider<?> classProvider() {
            return classProvider;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomLocation)) {
                return false;
            }
            CustomLocation other = (CustomLocation) o;
            return resourceProvider.equals(other.resourceProvider) && classProvider.equals(other.classProvider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceProvider, classProvider);
        }

        @Override
        public String toString() {
            return PREFIX + classProvider.getClass().getName();
        }
    }

    public static final class ClassPathLocation extends Location {
        /**
         * The prefix for classpath locations.
         */
        public static final String PREFIX = "classpath:";
        /**
         * The resource that contains the names of resources to provide. One line per resource.
         */
        public static final String RESOURCE_LIST_RESOURCE_NAME = "migratedb-resources.index";
        /**
         * The resource that contains the names of classes to provide. One line per class.
         */
        public static final String CLASS_LIST_RESOURCE_NAME = "migratedb-classes.index";

        private final String namePrefix;
        private final ClassLoader classLoader;

        public ClassPathLocation(String namePrefix, ClassLoader classLoader) {
            var trimmed = StringUtils.trimChar(namePrefix, '/');
            this.namePrefix = trimmed.isEmpty() ? "" : trimmed + "/";
            this.classLoader = classLoader;
        }

        @Override
        public ResourceProvider resourceProvider() {
            return new ClassPathResourceProvider(classLoader, readLines(RESOURCE_LIST_RESOURCE_NAME));
        }

        @Override
        public ClassProvider<?> classProvider() {
            return new ClassProvider<>() {
                private final List<Class<?>> classes = readLines(CLASS_LIST_RESOURCE_NAME)
                        .stream()
                        .map(it -> ClassUtils.loadClass(it, classLoader))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableList());

                @Override
                public Collection<Class<?>> getClasses() {
                    return classes;
                }
            };
        }

        @Override
        public boolean exists() {
            return classLoader.getResource(namePrefix + RESOURCE_LIST_RESOURCE_NAME) != null ||
                    classLoader.getResource(namePrefix + CLASS_LIST_RESOURCE_NAME) != null;
        }

        private List<String> readLines(String relativeResourceName) {
            var result = new ArrayList<String>();
            try {
                for (var resource : Collections.list(classLoader.getResources(namePrefix + relativeResourceName))) {
                    try (var reader = new BufferedReader(new InputStreamReader(resource.openStream(), UTF_8))) {
                        reader.lines().sequential().forEach(result::add);
                    }
                }
                return result;
            } catch (IOException e) {
                throw new MigrateDbException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ClassPathLocation)) {
                return false;
            }
            ClassPathLocation other = (ClassPathLocation) o;
            return namePrefix.equals(other.namePrefix) && classLoader.equals(other.classLoader);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namePrefix, classLoader);
        }

        @Override
        public String toString() {
            return PREFIX + namePrefix;
        }
    }

    public static final class FileSystemLocation extends Location {
        /**
         * The prefix for filesystem locations.
         */
        public static final String PREFIX = "filesystem:";

        private final Path baseDirectory;

        public FileSystemLocation(Path baseDirectory) {
            this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
        }

        @Override
        public ResourceProvider resourceProvider() {
            return new FileSystemResourceProvider(baseDirectory);
        }

        @Override
        public ClassProvider<?> classProvider() {
            return ClassProvider.noClasses();
        }

        @Override
        public boolean exists() {
            return Files.isDirectory(baseDirectory);
        }

        /**
         * @return The absolute, normalized base directory path.
         */
        public Path getBaseDirectory() {
            return baseDirectory;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileSystemLocation)) {
                return false;
            }
            FileSystemLocation other = (FileSystemLocation) o;
            return baseDirectory.equals(other.baseDirectory);
        }

        @Override
        public int hashCode() {
            return baseDirectory.hashCode();
        }

        @Override
        public String toString() {
            return PREFIX + baseDirectory;
        }
    }
}
