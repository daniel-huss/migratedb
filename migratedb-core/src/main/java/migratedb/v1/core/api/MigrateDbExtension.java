/*
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.api;

import migratedb.v1.core.api.configuration.FluentConfiguration;
import migratedb.v1.core.api.internal.database.base.DatabaseType;

import java.util.Collections;
import java.util.Set;

/**
 * Third-party extensions can contribute additional features through implementations of this interface. To enable an
 * extension, use {@link FluentConfiguration#useExtension(MigrateDbExtension)}.
 */
public interface MigrateDbExtension {
    /**
     * @return A human-readable English description of this extension.
     */
    String getDescription();

    /**
     * @return The database types contributed by this extension.
     */
    default Set<DatabaseType> getDatabaseTypes() {
        return Collections.emptySet();
    }

    /**
     * @return Converters of extension-specific configuration properties (i.e. String-based configuration) to the
     * corresponding internal data structures (one per data structure type).
     */
    default Set<ConfigPropertiesConverter> getConfigPropertiesConverters() {
        return Collections.emptySet();
    }
}
