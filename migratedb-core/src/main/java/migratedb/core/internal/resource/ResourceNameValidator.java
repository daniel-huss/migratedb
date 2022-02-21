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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.logging.Log;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.util.StringUtils;

public class ResourceNameValidator {
    private static final Log LOG = Log.getLog(ResourceNameValidator.class);

    /**
     * Validates the names of all SQL resources returned by the ResourceProvider
     *
     * @param provider      The ResourceProvider to validate
     * @param configuration The configuration to use
     */
    public void validateSQLMigrationNaming(ResourceProvider provider, Configuration configuration) {

        List<String> errorsFound = new ArrayList<>();
        ResourceNameParser resourceNameParser = new ResourceNameParser(configuration);

        for (Resource resource : getAllSqlResources(provider, configuration)) {
            String filename = resource.getName();
            LOG.debug("Validating " + filename);
            ResourceName result = resourceNameParser.parse(filename);
            if (!result.isValid()) {
                errorsFound.add(result.getValidityMessage());
            }
        }

        if (!errorsFound.isEmpty()) {
            throw new MigrateDbException(
                    "Invalid SQL filenames found:\n" + StringUtils.collectionToDelimitedString(errorsFound, "\n"));
        }
    }

    private Collection<Resource> getAllSqlResources(ResourceProvider provider, Configuration configuration) {
        return provider.getResources("", configuration.getSqlMigrationSuffixes());
    }
}
