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
import migratedb.core.api.internal.parser.Parser;
import migratedb.core.api.internal.sqlscript.SqlScriptMetadata;
import migratedb.core.api.logging.Log;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.configuration.ConfigUtils;
import migratedb.core.internal.parser.PlaceholderReplacingReader;

public class SqlScriptMetadataImpl implements SqlScriptMetadata {
    private static final Log LOG = Log.getLog(SqlScriptMetadataImpl.class);
    private static final String EXECUTE_IN_TRANSACTION = "executeInTransaction";
    private static final String ENCODING = "encoding";
    private static final String PLACEHOLDER_REPLACEMENT = "placeholderReplacement";
    private static final String SHOULD_EXECUTE = "shouldExecute";

    private final Boolean executeInTransaction;
    private final String encoding;
    private final boolean placeholderReplacement;
    private final boolean shouldExecute;

    private SqlScriptMetadataImpl(Map<String, String> metadata) {
        // Make copy to prevent removing elements from the original
        var copy = new HashMap<>(metadata);

        this.executeInTransaction = ConfigUtils.removeBoolean(copy, EXECUTE_IN_TRANSACTION);
        this.encoding = copy.remove(ENCODING);

        this.placeholderReplacement = Boolean.parseBoolean(copy.getOrDefault(PLACEHOLDER_REPLACEMENT, "true"));
        copy.remove(PLACEHOLDER_REPLACEMENT);

        this.shouldExecute = true;

        ConfigUtils.reportUnrecognisedProperties(copy, null);
    }

    @Override
    public Boolean executeInTransaction() {
        return executeInTransaction;
    }

    @Override
    public String encoding() {
        return encoding;
    }

    @Override
    public boolean placeholderReplacement() {
        return placeholderReplacement;
    }

    @Override
    public boolean shouldExecute() {
        return shouldExecute;
    }

    public static boolean isMultilineBooleanExpression(String line) {
        return !line.startsWith(SqlScriptMetadataImpl.SHOULD_EXECUTE) && (line.contains("==") || line.contains("!="));
    }

    public static SqlScriptMetadata fromResource(Resource resource, Parser parser) {
        if (resource != null) {
            LOG.debug("Found script configuration: " + resource.getName());
            return new SqlScriptMetadataImpl(loadConfiguration(
                PlaceholderReplacingReader.create(parser.getConfiguration(),
                                                  parser.getParsingContext(),
                                                  resource.read(parser.getConfiguration().getEncoding()))));
        }
        return new SqlScriptMetadataImpl(new HashMap<>());
    }

    public static Resource getMetadataResource(ResourceProvider resourceProvider, Resource resource) {
        if (resourceProvider == null) {
            return null;
        }
        return resourceProvider.getResource(resource.getName() + ".conf");
    }
}
