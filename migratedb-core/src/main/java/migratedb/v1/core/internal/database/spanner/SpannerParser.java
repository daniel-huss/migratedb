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
package migratedb.v1.core.internal.database.spanner;

import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.parser.BaseParser;
import migratedb.v1.core.internal.parser.Token;

import java.util.List;

public class SpannerParser extends BaseParser {
    private static final Log LOG = Log.getLog(SpannerParser.class);

    public SpannerParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }

    @Override
    protected char getIdentifierQuote() {
        return '`';
    }

    @Override
    protected char getAlternativeIdentifierQuote() {
        return '\"';
    }

    @Override
    protected Boolean detectCanExecuteInTransaction(String simplifiedStatement, List<Token> keywords) {
        LOG.debug("checking if [" + simplifiedStatement + "] can run in transaction");
        // MigrateDb tries to do hold transaction in which migration will happen
        return false;
    }
}
