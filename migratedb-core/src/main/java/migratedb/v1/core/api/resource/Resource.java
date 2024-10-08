/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.api.resource;

import java.io.Reader;
import java.nio.charset.Charset;

/**
 * A textual resource (such as a .sql file) used by MigrateDB.
 */
public interface Resource {
    /**
     * @return The path of this resource, separated by forward slashes ({@code "/"} on all platforms. Never ends with a
     * slash.
     */
    String getName();

    /**
     * @return Just the last component of the name of this resource, the "file name".
     */
    default String getLastNameComponent() {
        return lastNameComponentOf(getName());
    }

    /**
     * @return A hint that describes the physical location of this resource (like a full path to a file on disk)
     */
    String describeLocation();

    /**
     * @return The contents of the resource.
     */
    Reader read(Charset charset);

    static String lastNameComponentOf(String name) {
        var lastIndex = name.lastIndexOf('/');
        if (lastIndex == -1) {
            return name;
        }
        return name.substring(lastIndex + 1);
    }
}
