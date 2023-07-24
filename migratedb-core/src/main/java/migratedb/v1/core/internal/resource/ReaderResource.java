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
package migratedb.v1.core.internal.resource;

import migratedb.v1.core.api.resource.Resource;

import java.io.Reader;
import java.nio.charset.Charset;

public class ReaderResource implements Resource {
    private final Reader reader;
    private final String name;

    public ReaderResource(String name, Reader reader) {
        this.name = name;
        this.reader = reader;
    }

    @Override
    public Reader read(Charset charset) {
        return reader;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String describeLocation() {
        return "<Reader>";
    }

    @Override
    public String toString() {
        return describeLocation();
    }
}
