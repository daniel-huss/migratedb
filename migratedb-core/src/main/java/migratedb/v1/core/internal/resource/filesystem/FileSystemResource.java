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
package migratedb.v1.core.internal.resource.filesystem;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.resource.Resource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileSystemResource implements Resource {
    private final Path file;
    private final String relativeName;

    public FileSystemResource(Path file, Path relativeTo) {
        this.file = file.toAbsolutePath().normalize();
        var relativized = tryRelativize(file, relativeTo.toAbsolutePath().normalize());
        var segments = new ArrayList<String>();
        for (var segment : relativized) {
            segments.add(segment.toString());
        }
        this.relativeName = String.join("/", segments);
    }

    private static Path tryRelativize(Path file, Path relativeTo) {
        if (file.equals(relativeTo)) return file;
        try {
            return relativeTo.relativize(file);
        } catch (RuntimeException e) {
            return file;
        }
    }

    @Override
    public Reader read(Charset charset) {
        try {
            return Files.newBufferedReader(file, charset);
        } catch (IOException e) {
            throw new MigrateDbException("Cannot open file system resource " + getName(), e);
        }
    }

    @Override
    public String getName() {
        return relativeName;
    }

    @Override
    public String describeLocation() {
        return "file: " + file;
    }

    @Override
    public String toString() {
        return describeLocation();
    }
}
