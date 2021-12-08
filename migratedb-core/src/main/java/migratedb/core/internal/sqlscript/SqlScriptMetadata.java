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
package migratedb.core.internal.sqlscript;

import static migratedb.core.internal.configuration.ConfigUtils.loadConfiguration;

import java.util.HashMap;
import java.util.Map;
import migratedb.core.api.ResourceProvider;
import migratedb.core.api.logging.Log;
import migratedb.core.api.resource.LoadableResource;
import migratedb.core.internal.configuration.ConfigUtils;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.PlaceholderReplacingReader;

public class SqlScriptMetadata {
    private static final Log LOG = Log.getLog(SqlScriptMetadata.class);
    private static final String EXECUTE_IN_TRANSACTION = "executeInTransaction";
    private static final String ENCODING = "encoding";
    private static final String SHOULD_EXECUTE = "shouldExecute";

    private final Boolean executeInTransaction;
    private final String encoding;
    private final boolean shouldExecute;

    private SqlScriptMetadata(Map<String, String> metadata) {
        // Make copy to prevent removing elements from the original
        metadata = new HashMap<>(metadata);
        this.executeInTransaction = ConfigUtils.removeBoolean(metadata, EXECUTE_IN_TRANSACTION);
        this.encoding = metadata.remove(ENCODING);
        this.shouldExecute = true;

        ConfigUtils.reportUnrecognisedProperties(metadata, null);
    }

    public Boolean executeInTransaction() {
        return executeInTransaction;
    }

    public String encoding() {
        return encoding;
    }

    public boolean shouldExecute() {
        return shouldExecute;
    }

    public static boolean isMultilineBooleanExpression(String line) {
        return !line.startsWith(SHOULD_EXECUTE) && (line.contains("==") || line.contains("!="));
    }

    public static SqlScriptMetadata fromResource(LoadableResource resource, Parser parser) {
        if (resource != null) {
            LOG.debug("Found script configuration: " + resource.getFilename());
            if (parser == null) {
                return new SqlScriptMetadata(loadConfiguration(resource.read()));
            }
            return new SqlScriptMetadata(loadConfiguration(
                PlaceholderReplacingReader.create(parser.configuration, parser.parsingContext, resource.read())));
        }
        return new SqlScriptMetadata(new HashMap<>());
    }

    public static LoadableResource getMetadataResource(ResourceProvider resourceProvider, LoadableResource resource) {
        if (resourceProvider == null) {
            return null;
        }
        return resourceProvider.getResource(resource.getRelativePath() + ".conf");
    }
}
