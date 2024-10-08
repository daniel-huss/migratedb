/*
 * Copyright 2022-2024 The MigrateDB contributors
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

package migratedb.v1.core.internal.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

public final class FileOutputStreamFactory implements Supplier<OutputStream> {
    private final Path file;

    public FileOutputStreamFactory(Path file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileOutputStreamFactory)) {
            return false;
        }
        FileOutputStreamFactory other = (FileOutputStreamFactory) o;
        return Objects.equals(file, other.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public OutputStream get() {
        try {
            var parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return Files.newOutputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
