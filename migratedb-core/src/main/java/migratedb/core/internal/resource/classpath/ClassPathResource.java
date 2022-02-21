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
package migratedb.core.internal.resource.classpath;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.resource.Resource;

public class ClassPathResource implements Resource {
    private final ClassLoader classLoader;
    private final String name;

    public ClassPathResource(String name, ClassLoader classLoader) {
        this.name = name;
        this.classLoader = classLoader;
    }

    @Override
    public Reader read(Charset charset) {
        var stream = classLoader.getResourceAsStream(name);
        if (stream == null) throw new MigrateDbException("No such resource: " + name);
        return new InputStreamReader(stream, charset);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String describeLocation() {
        var resource = classLoader.getResource(name);
        return "classpath: " + name + " (" + resource + ")";
    }

    @Override
    public String toString() {
        return describeLocation();
    }
}
