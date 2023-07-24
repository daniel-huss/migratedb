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

package migratedb.v1.core.internal.resource.classpath;

import migratedb.v1.core.api.resource.Resource;
import migratedb.v1.core.internal.resource.NameListResourceProvider;

import java.util.List;

public class ClassPathResourceProvider extends NameListResourceProvider {
    private final ClassLoader classLoader;

    public ClassPathResourceProvider(ClassLoader classLoader, List<String> resourceNames) {
        super(resourceNames);
        this.classLoader = classLoader;
    }

    @Override
    protected Resource toResource(String resourceName) {
        return new ClassPathResource(resourceName, classLoader);
    }
}
