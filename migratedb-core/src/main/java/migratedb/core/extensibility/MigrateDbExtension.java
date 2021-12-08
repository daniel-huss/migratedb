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
package migratedb.core.extensibility;

import java.util.Collections;
import java.util.Set;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.internal.database.DatabaseType;

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
}
