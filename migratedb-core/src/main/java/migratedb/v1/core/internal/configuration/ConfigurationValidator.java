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
package migratedb.v1.core.internal.configuration;

import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.configuration.Configuration;

import java.util.Locale;

public class ConfigurationValidator {
    public void validate(Configuration configuration) {
        if (configuration.getDataSource() == null) {
            throw new MigrateDbException(
                    "Unable to connect to the database, because no data source was configured",
                    ErrorCode.CONFIGURATION);
        }

        for (String key : configuration.getPlaceholders().keySet()) {
            if (key.toLowerCase(Locale.ENGLISH).startsWith("migratedb:")) {
                throw new MigrateDbException("Invalid placeholder ('migratedb:' prefix is reserved): " + key,
                        ErrorCode.CONFIGURATION);
            }
        }
    }
}
