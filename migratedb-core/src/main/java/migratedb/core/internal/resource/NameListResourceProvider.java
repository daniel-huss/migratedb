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

package migratedb.core.internal.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.util.StringUtils;

public abstract class NameListResourceProvider implements ResourceProvider {
    private final String[] names;

    public NameListResourceProvider(Collection<String> names) {
        this.names = names.toArray(String[]::new);
        Arrays.sort(this.names);
    }

    @Override
    public final Resource getResource(String name) {
        var idx = Arrays.binarySearch(names, name);
        if (idx < 0) {
            return null;
        }
        return toResource(names[idx]);
    }

    @Override
    public final Collection<Resource> getResources(String prefix, String... suffixes) {
        return Arrays.stream(names)
                     .filter(it -> StringUtils.startsAndEndsWith(lastNameComponentOf(it), prefix, suffixes))
                     .map(this::toResource)
                     .collect(Collectors.toList());
    }

    protected abstract Resource toResource(String name);

    private String lastNameComponentOf(String name) {
        var lastIndex = name.lastIndexOf('/');
        if (lastIndex == -1) {
            return name;
        }
        if (lastIndex >= name.length()) {
            return "";
        }
        return name.substring(lastIndex + 1);
    }
}
