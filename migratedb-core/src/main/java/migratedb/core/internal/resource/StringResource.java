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
package migratedb.core.internal.resource;

import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import migratedb.core.api.resource.Resource;

public class StringResource implements Resource {
    private final String str;
    private final String name;

    public StringResource(String name, String str) {
        this.name = name;
        this.str = str;
    }

    @Override
    public Reader read(Charset charset) {
        return new StringReader(str);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String describeLocation() {
        return "<String>";
    }

    @Override
    public String toString() {
        return describeLocation();
    }
}
