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

package migratedb.core.internal.resource.filesystem;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.util.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Finds resources in a base directory and its non-hidden sub-directories.
 */
public class FileSystemResourceProvider implements ResourceProvider {
    private final Path baseDir;

    public FileSystemResourceProvider(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    @Override
    public @Nullable Resource getResource(String name) {
        var file = baseDir.resolve(name);
        if (Files.exists(file)) {
            return new FileSystemResource(baseDir.resolve(name), baseDir);
        } else {
            return null;
        }
    }

    @Override
    public Collection<Resource> getResources(String prefix, String... suffixes) {
        var result = new ArrayList<Resource>();
        try {
            Files.walkFileTree(baseDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 100, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isHidden(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (StringUtils.startsAndEndsWith(file.getFileName().toString(), prefix, suffixes)) {
                        result.add(new FileSystemResource(file, baseDir));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException ignored) {
        } catch (IOException e) {
            throw new MigrateDbException("Failed to walk directory " + baseDir, e);
        }
        return result;
    }
}
